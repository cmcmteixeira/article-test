package elevio.service

import cats.effect._
import cats.implicits._
import com.itv.bucky.AmqpClient
import com.itv.bucky.kamonSupport._
import com.typesafe.config.ConfigFactory
import doobie.hikari.HikariTransactor
import elevio.service.app.{App, Config, _}
import kamon.Kamon
import kamon.system.SystemMetrics

import scala.concurrent.ExecutionContext

object Main extends IOApp.WithContext {

  def config: SyncIO[Config] =
    SyncIO(
      ConfigFactory.load()
    ).map(c => buildConfig(c).unsafeRunSync)

  override protected def executionContextResource: Resource[SyncIO, ExecutionContext] =
    Resource
      .pure[SyncIO, Unit](())
      .evalMap(_ => config)
      .flatMap(mainThreadPool)
      .map(ExecutionContext.fromExecutor(_))

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val ec: ExecutionContext = executionContext
    (for {
      config <- Resource.liftF(config.to[IO])
      _      <- Resource.liftF(IO(Kamon.loadReportersFromConfig()))
      //_              <- Resource.make(IO(Kamon.addReporter(new InfluxDBReporter())))(r => IO(r.cancel()).void)
      _              <- Resource.make(IO(SystemMetrics.startCollecting()))(_ => IO.unit)
      amqpClient     <- AmqpClient.apply[IO](config.amqp).map(_.withKamonSupport(logging = true))
      client         <- httpClient(config)(executionContext, contextShift)
      dbConnectionEc <- dbConnectionThreadPool(config).map(ExecutionContext.fromExecutor)
      dbTransactEc   <- dbConnectionThreadPool(config).map(ExecutionContext.fromExecutor)
      transactor <- HikariTransactor
        .newHikariTransactor[IO](config.db.driverClassName, config.db.url, config.db.user, config.db.pass, dbConnectionEc, dbTransactEc)
    } yield {
      App(executionContext, contextShift, timer, transactor, client, amqpClient, config)
    }).use(app => app.handlers *> app.migrations *> server(app).use(_ => IO.never))
  }

}

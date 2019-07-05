package elevio.service

import cats.effect._
import cats.implicits._
import com.itv.bucky.AmqpClient
import com.itv.bucky.kamonSupport._
import com.typesafe.config.ConfigFactory
import doobie.hikari.HikariTransactor
import elevio.service.app.{App, Config, _}
import kamon.Kamon
import kamon.influxdb.InfluxDBReporter
import kamon.system.SystemMetrics

import scala.concurrent.ExecutionContext

object Main extends IOApp.WithContext {

  def config: SyncIO[Config] =
    SyncIO(
      ConfigFactory.load()
    ).map(c => buildConfig(c).unsafeRunSync) //this won't be an issue as if this hangs indefinitely, the project will not start

  override protected def executionContextResource: Resource[SyncIO, ExecutionContext] =
    Resource
      .pure[SyncIO, Unit](())
      .evalMap(_ => config)
      .flatMap(c => mainThreadPool(c.mainThreadPool))
      .map(ExecutionContext.fromExecutor(_))

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val ec: ExecutionContext = executionContext
    (for {
      config         <- Resource.liftF(config.to[IO])
      _              <- Resource.liftF(IO(Kamon.loadReportersFromConfig()))
      _              <- Resource.make(IO(Kamon.addReporter(new InfluxDBReporter())))(r => IO(r.cancel()).void)
      _              <- Resource.make(IO(SystemMetrics.startCollecting()))(_ => IO.unit)
      amqpClient     <- AmqpClient.apply[IO](config.amqp).map(_.withKamonSupport(logging = true))
      client         <- httpClient(config.httpClient)(executionContext, contextShift)
      dbConnectionEc <- dbConnectionThreadPool(config.dbThreadPool).map(ExecutionContext.fromExecutor)
      dbTransactEc   <- dbTransactionThreadPool().map(ExecutionContext.fromExecutor)
      transactor <- HikariTransactor
        .newHikariTransactor[IO](config.db.driverClassName, config.db.url, config.db.user, config.db.pass, dbConnectionEc, dbTransactEc)
      app = App(executionContext, contextShift, timer, transactor, client, amqpClient, config)
      _ <- Resource.liftF(app.migrations)
      _ <- Resource.liftF(app.declarations)
      _ <- app.handlers
    } yield app).use(app => server(app).use(_ => IO.never))
  }

}

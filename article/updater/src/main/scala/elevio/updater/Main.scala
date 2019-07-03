package elevio.updater

import cats.effect.{ExitCode, IO, IOApp, Resource, SyncIO}

import scala.concurrent.ExecutionContext
import cats.implicits._
import cats.effect._
import com.itv.bucky.publish._
import com.itv.bucky.circe._
import com.itv.bucky.AmqpClient
import com.typesafe.config.ConfigFactory
import elevio.updater.app.Config
import kamon.Kamon
import kamon.influxdb.InfluxDBReporter
import kamon.system.SystemMetrics
import elevio.updater.app._
import com.itv.bucky.kamonSupport._

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
      config     <- Resource.liftF(config.to[IO])
      _          <- Resource.liftF(IO(Kamon.loadReportersFromConfig()))
      _          <- Resource.make(IO(Kamon.addReporter(new InfluxDBReporter())))(r => IO(r.cancel()).void)
      _          <- Resource.make(IO(SystemMetrics.startCollecting()))(_ => IO.unit)
      amqpClient <- AmqpClient.apply[IO](config.amqp).map(_.withKamonSupport(logging = true))
      client     <- httpClient(config.httpClient)(executionContext, contextShift)
      app = App(executionContext, contextShift, timer, client, amqpClient, config)
      _ <- Resource.liftF(app.declarations)
    } yield app).use(app => server(app).use(_ => app.scheduler *> IO(ExitCode.Success)))
  }

}

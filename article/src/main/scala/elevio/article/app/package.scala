package elevio.article

import java.util.concurrent.{ExecutorService, Executors}

import cats.effect.{ContextShift, IO, Resource, SyncIO, Timer}
import com.itv.bucky.AmqpClientConfig
import com.typesafe.config.{Config => RawConfig}
import net.ceedubs.ficus.Ficus._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder
import net.ceedubs.ficus.readers.namemappers.implicits.hyphenCase
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import cats.implicits._
import kamon.executors.util.ContextAwareExecutorService
import kamon.http4s.middleware.client.{KamonSupport => KClient}
import kamon.http4s.middleware.server.{KamonSupport => KServer}
import org.http4s.implicits._

import scala.concurrent.ExecutionContext
import kamon.executors.{Executors => KExecutors}

package object app {

  def buildConfig(config: RawConfig): IO[Config] =
    IO {
      val threadPool = config.as[ThreadPoolConfig]("thread-pool")
      val amqp       = config.as[AmqpClientConfig]("amqp")
      val db         = config.as[DatabaseConfig]("db")
      val httpServer = config.as[HttpServerConfig]("http-server")
      val httpClient = config.as[HttpClientConfig]("http-client")
      Config(threadPool, amqp, db, httpServer, httpClient)
    }

  def httpClient(config: Config)(implicit ec: ExecutionContext, cs: ContextShift[IO]): Resource[IO, Client[IO]] =
    BlazeClientBuilder[IO](ec)
      .withExecutionContext(ec)
      .withMaxWaitQueueLimit(config.httpClient.maxWaitQueue)
      .withIdleTimeout(config.httpClient.idleTimeout)
      .withResponseHeaderTimeout(config.httpClient.responseHeaderTimeout)
      .resource
      .map(KClient(_))

  def mainThreadPool(config: Config): Resource[SyncIO, ExecutorService] =
    Resource
      .make(
        for {
          rawExecutor     <- SyncIO(Executors.newFixedThreadPool(config.threadPool.size))
          instrumentedExc <- SyncIO(ContextAwareExecutorService(rawExecutor))
          registration    <- SyncIO(KExecutors.register("main", rawExecutor))
        } yield (rawExecutor, instrumentedExc, registration)
      ) {
        case (executor, _, registration) => SyncIO(executor.shutdown()) *> SyncIO(registration.cancel()).void
      }
      .map(_._2)

  def dbConnectionThreadPool(config: Config): Resource[IO, ExecutorService] =
    Resource
      .make(
        for {
          rawExecutor     <- IO(Executors.newFixedThreadPool(config.threadPool.dbConnectionSize))
          instrumentedExc <- IO(ContextAwareExecutorService(rawExecutor))
          registration    <- IO(KExecutors.register("db-connection", rawExecutor))
        } yield (rawExecutor, instrumentedExc, registration)
      ) {
        case (executor, _, registration) => IO(executor.shutdown()) *> IO(registration.cancel()).void
      }
      .map(_._2)

  def dbTransactionThreadPool(config: Config): Resource[IO, ExecutorService] =
    Resource
      .make(
        for {
          rawExecutor     <- IO(Executors.newCachedThreadPool())
          instrumentedExc <- IO(ContextAwareExecutorService(rawExecutor))
          registration    <- IO(KExecutors.register("db-transaction", rawExecutor))
        } yield (rawExecutor, instrumentedExc, registration)
      ) {
        case (executor, _, registration) => IO(executor.shutdown()) *> IO(registration.cancel()).void
      }
      .map(_._2)

  def blazeServerBuilder(config: Config)(implicit timer: Timer[IO], cs: ContextShift[IO], ec: ExecutionContext): BlazeServerBuilder[IO] =
    BlazeServerBuilder[IO]
      .withExecutionContext(implicitly)
      .bindHttp(config.httpServer.port, config.httpServer.address)

  def server(app: App) =
    BlazeServerBuilder(IO.ioConcurrentEffect(app.cs), app.timer)
      .withHttpApp(KServer(app.routes).orNotFound)
      .withExecutionContext(app.ec)
      .bindHttp(app.config.httpServer.port, app.config.httpServer.address)
      .resource

}

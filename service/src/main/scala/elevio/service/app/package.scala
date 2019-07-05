package elevio.service

import com.itv.bucky.AmqpClientConfig
import com.typesafe.config.{Config => RawConfig}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.namemappers.implicits.hyphenCase
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import elevio.common.httpclient.ElevioArticleClient.ElevioArticleClientConfig
import elevio.service.services.ArticleService.ArticleServiceConfig
import kamon.http4s.middleware.client.{KamonSupport => KClient}
import kamon.http4s.middleware.server.{KamonSupport => KServer}
import org.http4s.implicits._
import java.util.concurrent.{ExecutorService, Executors}

import cats.effect.{ContextShift, IO, Resource, SyncIO, Timer}
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import kamon.executors.util.ContextAwareExecutorService
import kamon.executors.{Executors => KExecutors}
import cats.implicits._
import elevio.common.model.{ApiKey, ItemsPerPage, JWT}
import net.ceedubs.ficus.readers.{AnyValReaders, StringReader, ValueReader}
import org.http4s.Uri
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

package object app {

  def mainThreadPool(config: ThreadPoolConfig): Resource[SyncIO, ExecutorService] =
    Resource
      .make(
        for {
          rawExecutor     <- SyncIO(Executors.newFixedThreadPool(config.size))
          instrumentedExc <- SyncIO(ContextAwareExecutorService(rawExecutor))
          registration    <- SyncIO(KExecutors.register("main", rawExecutor))
        } yield (rawExecutor, instrumentedExc, registration)
      ) {
        case (executor, _, registration) => SyncIO(executor.shutdown()) *> SyncIO(registration.cancel()).void
      }
      .map(_._2)

  def dbConnectionThreadPool(config: ThreadPoolConfig): Resource[IO, ExecutorService] =
    Resource
      .make(
        for {
          rawExecutor     <- IO(Executors.newFixedThreadPool(config.size))
          instrumentedExc <- IO(ContextAwareExecutorService(rawExecutor))
          registration    <- IO(KExecutors.register("db-connection", rawExecutor))
        } yield (rawExecutor, instrumentedExc, registration)
      ) {
        case (executor, _, registration) => IO(executor.shutdown()) *> IO(registration.cancel()).void
      }
      .map(_._2)

  def dbTransactionThreadPool(): Resource[IO, ExecutorService] =
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

  def httpClient(config: HttpClientConfig)(implicit ec: ExecutionContext, cs: ContextShift[IO]): Resource[IO, Client[IO]] =
    BlazeClientBuilder[IO](ec)
      .withExecutionContext(ec)
      .withMaxWaitQueueLimit(config.maxWaitQueue)
      .withIdleTimeout(config.idleTimeout)
      .withResponseHeaderTimeout(config.responseHeaderTimeout)
      .resource
      .map(KClient(_))

  def buildConfig(config: RawConfig): IO[Config] =
    IO {
      implicit val uriReader: ValueReader[Uri]             = StringReader.stringValueReader.map(Uri.unsafeFromString)
      implicit val jwtReader: ValueReader[JWT]             = StringReader.stringValueReader.map(JWT(_))
      implicit val apiKey: ValueReader[ApiKey]             = StringReader.stringValueReader.map(ApiKey(_))
      implicit val itemsPerPage: ValueReader[ItemsPerPage] = AnyValReaders.intValueReader.map(ItemsPerPage(_))
      val mainThreadPool                                   = config.as[ThreadPoolConfig]("main-thread-pool")
      val dbThreadPool                                     = config.as[ThreadPoolConfig]("db-thread-pool")
      val amqp                                             = config.as[AmqpClientConfig]("amqp")
      val db                                               = config.as[DatabaseConfig]("db")
      val httpServer                                       = config.as[HttpServerConfig]("http-server")
      val httpClient                                       = config.as[HttpClientConfig]("http-client")
      val elevioService                                    = config.as[ElevioArticleClientConfig]("elevio-service")
      val articleSerConfig                                 = config.as[ArticleServiceConfig]("article-service")
      Config(mainThreadPool, dbThreadPool, amqp, db, httpServer, httpClient, elevioService, articleSerConfig)
    }

  def blazeServerBuilder(config: HttpServerConfig)(implicit timer: Timer[IO], cs: ContextShift[IO], ec: ExecutionContext): BlazeServerBuilder[IO] =
    BlazeServerBuilder[IO]
      .withExecutionContext(implicitly)
      .bindHttp(config.port, config.address)

  def server(app: App) =
    BlazeServerBuilder(IO.ioConcurrentEffect(app.cs), app.timer)
      .withHttpApp(KServer(app.routes).orNotFound)
      .withExecutionContext(app.ec)
      .bindHttp(app.config.httpServer.port, app.config.httpServer.address)
      .resource
}

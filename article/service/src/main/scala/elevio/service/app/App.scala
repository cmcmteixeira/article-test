package elevio.service.app

import cats.effect.{ContextShift, IO, Resource, Timer}
import com.itv.bucky.AmqpClient
import org.http4s.client.Client
import org.http4s.{HttpRoutes, Uri}

import scala.concurrent.ExecutionContext
import elevio.service.health.HealthCheckService
import elevio.service.routes.{AppStatusRoutes, ArticleRoutes, InternalArticleRoutes}
import doobie.hikari.HikariTransactor
import elevio.common.httpclient.ElevioArticleClient
import elevio.service.repository.ArticleRepository
import elevio.service.services.ArticleService
import org.flywaydb.core.Flyway
import org.http4s.server.Router

trait App {
  def ec: ExecutionContext
  def cs: ContextShift[IO]
  def timer: Timer[IO]
  def db: HikariTransactor[IO]
  def client: Client[IO]
  def amqp: AmqpClient[IO]
  def config: Config
  def routes: HttpRoutes[IO]
  def handlers: IO[Unit]
  def declarations: IO[Unit]
  def migrations: IO[Unit] =
    IO {
      val flyway = new Flyway
      flyway.setDataSource(db.kernel)
      flyway.migrate()
      ()
    }
}

object App {
  def apply(
      _ec: ExecutionContext,
      _cs: ContextShift[IO],
      _timer: Timer[IO],
      _db: HikariTransactor[IO],
      _client: Client[IO],
      _amqp: AmqpClient[IO],
      _config: Config
  ): App = {
    val healthService     = HealthCheckService(_amqp, _db)
    val articleClient     = ElevioArticleClient(_client, _config.elevioService)
    val articleRepository = ArticleRepository(_db)
    val articleService    = ArticleService(articleClient, articleRepository, _config.articleService)
    val appRoutes = Router(
      "/_meta"             -> new AppStatusRoutes(healthService).routes,
      "/articles"          -> new ArticleRoutes(articleService).routes,
      "/internal/articles" -> new InternalArticleRoutes(articleService).routes
    )

    new App {
      override def ec: ExecutionContext         = _ec
      override def cs: ContextShift[IO]         = _cs
      override def timer: Timer[IO]             = _timer
      override def db: HikariTransactor[IO]     = _db
      override def client: Client[IO]           = _client
      override def amqp: AmqpClient[IO]         = _amqp
      override def config: Config               = _config
      override def routes: HttpRoutes[IO]       = appRoutes
      override def handlers: Resource[IO, Unit] =

    }
  }

}

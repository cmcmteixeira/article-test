package elevio.updater.app

import cats.effect.{ContextShift, IO, Timer}
import com.itv.bucky.decl.{Exchange, Topic}
import com.itv.bucky.{AmqpClient, ExchangeName, RoutingKey}
import doobie.hikari.HikariTransactor
import elevio.common.httpclient.ElevioArticleClient
import elevio.common.model.ArticleUpdate
import elevio.updater.health.HealthCheckService
import elevio.updater.httpclients.InternalArticlesClient
import elevio.updater.routes.AppStatusRoutes
import elevio.updater.services.{ElevioWalker, InternalWalker}
import org.http4s.HttpRoutes
import org.http4s.client.Client
import org.http4s.server.Router
import com.itv.bucky.circe._
import elevio.common.tracing.KamonMetricsSupport

import scala.concurrent.ExecutionContext

trait App {
  def ec: ExecutionContext
  def cs: ContextShift[IO]
  def timer: Timer[IO]
  def client: Client[IO]
  def amqp: AmqpClient[IO]
  def config: Config
  def routes: HttpRoutes[IO]
  def declarations: IO[Unit]
  def scheduler: IO[Unit]
}

object App extends KamonMetricsSupport {
  def apply(
      _ec: ExecutionContext,
      _cs: ContextShift[IO],
      _timer: Timer[IO],
      _client: Client[IO],
      _amqp: AmqpClient[IO],
      _config: Config
  ): App = {
    val articleExchange = ExchangeName("articles")
    val routingKey      = RoutingKey("update")

    val elevioClient          = ElevioArticleClient(_client, _config.elevioService)
    val internalArticleClient = InternalArticlesClient(_client, _config.internalService)
    val healthService         = HealthCheckService(_amqp)
    val publisher             = _amqp.publisherOf[ArticleUpdate](articleExchange, routingKey)
    val appRoutes = Router(
      "/_meta" -> new AppStatusRoutes(healthService).routes,
    )

    val walker = for {
      _ <- measured[Unit]("internal")(InternalWalker(_config.internalWalkerConfig, elevioClient, internalArticleClient, publisher)(_cs, _timer).run)
      _ <- measured[Unit]("elevio")(ElevioWalker(_config.elevioWalkerConfig, elevioClient, internalArticleClient, publisher)(_cs, _timer).run)
    } yield ()

    val appDeclarations = List(
      Exchange(articleExchange, Topic)
    )

    new App {
      override def ec: ExecutionContext   = _ec
      override def cs: ContextShift[IO]   = _cs
      override def timer: Timer[IO]       = _timer
      override def client: Client[IO]     = _client
      override def amqp: AmqpClient[IO]   = _amqp
      override def config: Config         = _config
      override def routes: HttpRoutes[IO] = appRoutes
      override def declarations: IO[Unit] = amqp.declare(appDeclarations)
      override def scheduler: IO[Unit]    = measured[Unit]("scheduler")(walker)
    }
  }

}

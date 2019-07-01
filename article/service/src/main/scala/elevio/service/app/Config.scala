package elevio.service.app

import com.itv.bucky.AmqpClientConfig
import elevio.common.httpclient.ElevioArticleClient.ElevioArticleClientConfig
import elevio.common.model.{ApiKey, JWT}
import elevio.service.services.ArticleService.ArticleServiceConfig
import org.http4s.Uri

import scala.concurrent.duration.FiniteDuration

case class ThreadPoolConfig(
    size: Int,
    shutdownTimeoutSeconds: Int,
    dbConnectionSize: Int
)
case class DatabaseConfig(
    driverClassName: String,
    url: String,
    user: String,
    pass: String
)

case class HttpServerConfig(
    port: Int,
    address: String
)
case class HttpClientConfig(
    maxWaitQueue: Int,
    idleTimeout: FiniteDuration,
    responseHeaderTimeout: FiniteDuration
)

case class Config(
    threadPool: ThreadPoolConfig,
    amqp: AmqpClientConfig,
    db: DatabaseConfig,
    httpServer: HttpServerConfig,
    httpClient: HttpClientConfig,
    elevioService: ElevioArticleClientConfig,
    articleService: ArticleServiceConfig
)

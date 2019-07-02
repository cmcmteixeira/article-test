package elevio.updater.app

import com.itv.bucky.AmqpClientConfig
import elevio.common.httpclient.ElevioArticleClient.ElevioArticleClientConfig
import elevio.common.model.ItemsPerPage
import elevio.updater.httpclients.InternalArticlesClient.InternalArticleClientConfig
import elevio.updater.services.ElevioWalker.ElevioWalkerConfig
import elevio.updater.services.InternalWalker.InternalWalkerConfig

import scala.concurrent.duration.FiniteDuration

case class ThreadPoolConfig(
    size: Int
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

case class ArticleServiceConfig(itemsPerPage: ItemsPerPage)

case class Config(
    mainThreadPool: ThreadPoolConfig,
    amqp: AmqpClientConfig,
    httpServer: HttpServerConfig,
    httpClient: HttpClientConfig,
    elevioService: ElevioArticleClientConfig,
    internalService: InternalArticleClientConfig,
    internalWalkerConfig: InternalWalkerConfig,
    elevioWalkerConfig: ElevioWalkerConfig
)

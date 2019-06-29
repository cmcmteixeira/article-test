package elevio.article.app

import com.itv.bucky.AmqpClientConfig

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
    httpClient: HttpClientConfig
)

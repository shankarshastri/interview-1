package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    cache: CacheConfig,
    oneFrameService: OneFrameServiceConfig
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class CacheConfig(expiryTime: FiniteDuration, size: Long)

case class OneFrameServiceConfig(url: String, token: String)
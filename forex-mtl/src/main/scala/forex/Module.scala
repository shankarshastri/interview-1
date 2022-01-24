package forex

import cats.effect._
import com.olegpy.meow.hierarchy._
import forex.config.ApplicationConfig
import forex.http._
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.services._
import forex.services.rates.interpreters.OneFrameApiService
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.middleware.{AutoSlash, Timeout}
import scalacache.Mode
import sttp.client.SttpBackend
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend

class Module[F[_]: Concurrent: Timer: ContextShift](config: ApplicationConfig) {

  private implicit val mode: Mode[F] = scalacache.CatsEffect.modes.async

  private implicit val backend: F[SttpBackend[F, Nothing, WebSocketHandler]] = AsyncHttpClientCatsBackend[F]()

  private val ratesApiService: OneFrameApiService[F] = new OneFrameApiService[F](config.cache, config.oneFrameService)

  private val routeHttpErrorHandler: ErrorHandler[F] = new ErrorHandler[F]

  private val ratesService: RatesService[F] = RatesServices.live[F](ratesApiService)

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(routeHttpErrorHandler.handle(http))
    }
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)
}

package forex.http

import cats.MonadError
import forex.programs.rates.errors.Error.RateLookupFailed
import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import sttp.client.SttpClientException.ConnectException

class ErrorHandler[F[_]](implicit M: MonadError[F, Exception]) extends Http4sDsl[F] {

  case class ErrorResponse(error: String)
  implicit val encoder: Encoder[ErrorResponse] = deriveEncoder
  implicit val decoder: Decoder[ErrorResponse] = deriveDecoder

  private val handler: Exception => F[Response[F]] = {
    case RateLookupFailed(msg) =>
      InternalServerError(ErrorResponse(msg).asJson)
    case _: MatchError =>
      BadRequest(ErrorResponse("Not a valid currency, please validate the query params").asJson)
    case _: ConnectException =>
      ServiceUnavailable(ErrorResponse("Forex service is down, though the proxy is active").asJson)
    case ex =>
      ex.printStackTrace()
      NotFound(ErrorResponse("Route doesn't exist").asJson)
  }

  def handle(routes: HttpRoutes[F]): HttpRoutes[F] =
    RouteHttpErrorHandler(routes)(handler)
}
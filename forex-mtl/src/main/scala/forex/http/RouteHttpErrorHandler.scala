package forex.http

import cats.ApplicativeError
import cats.data.{Kleisli, OptionT}
import cats.syntax.all._
import org.http4s.{HttpRoutes, Response}


object RouteHttpErrorHandler {
  def apply[F[_], E <: Throwable](routes: HttpRoutes[F])(handler: E => F[Response[F]])(implicit appError: ApplicativeError[F, E]): HttpRoutes[F] =
    Kleisli { req =>
      OptionT {
        routes.run(req).value.handleErrorWith(e => handler(e).map(Option(_)))
      }
    }
}

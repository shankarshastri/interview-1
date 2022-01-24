package forex.services.rates

import forex.services.rates.interpreters._

object Interpreters {
  def live[F[_]](service: OneFrameApiService[F]): Algebra[F] = new OneFrame[F](service)
}

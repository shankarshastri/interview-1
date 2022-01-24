package forex.services.rates.interpreters

import forex.domain._
import forex.services.rates.Algebra
import forex.services.rates.errors._

class OneFrame[F[_]](service: OneFrameApiService[F]) extends Algebra[F] {
  override def get(pair: Rate.Pair): F[Error Either Rate] =
    service.getFromAPI(pair)
}

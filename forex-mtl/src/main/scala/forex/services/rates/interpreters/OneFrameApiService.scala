package forex.services.rates.interpreters

import cats.effect._
import cats.implicits._
import com.github.benmanes.caffeine.cache.Caffeine
import forex.config.{CacheConfig, OneFrameServiceConfig}
import forex.domain._
import forex.services.rates.errors.Error.OneFrameLookupFailed
import forex.services.rates.errors._
import io.circe.generic.auto._
import scalacache._
import scalacache.caffeine._
import sttp.client._
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.circe.asJson

import java.time._

class OneFrameApiService[F[_] : Concurrent : Mode](cacheConfig: CacheConfig, oneFrameApiConfig: OneFrameServiceConfig)
                                                                 (implicit asyncHttpClientBackend: F[SttpBackend[F, Nothing, WebSocketHandler]]) {
  private val underlyingCaffeineCache = Caffeine.newBuilder()
    .expireAfterWrite(Duration.ofMinutes(cacheConfig.expiryTime.toMinutes))
    .maximumSize(cacheConfig.size).build[String, Entry[Rate]]
  private val caffeineCache = CaffeineCache[Rate](underlyingCaffeineCache)
  private val oneFrameApiInitReq = basicRequest.header("token", oneFrameApiConfig.token)

  case class Response(from: String, to: String, bid: Double, ask: Double, price: Double,
                      time_stamp: String)

  private def request(pair: Rate.Pair): Request[Either[String, String], Nothing] = {
    oneFrameApiInitReq.get(uri"${oneFrameApiConfig.url}?pair=${pair.from.show}${pair.to.show}")
  }

  private def fromApi(pair: Rate.Pair): F[Error Either Rate] = {
    asyncHttpClientBackend.flatMap {
      backend => {
        backend.send(request(pair).response(asJson[List[Response]])).map(e => {
          e.body.leftMap(e => OneFrameLookupFailed(e.getMessage)).flatMap(e => {
            if(e.isEmpty) {
              Either.left[Error, Rate](OneFrameLookupFailed("Currency conversion not possible"))
            } else {
              Either.right(Rate(pair, Price(e.head.price),
                Timestamp(OffsetDateTime.ofInstant(Instant.parse(e.head.time_stamp), ZoneId.of("UTC")))))
            }
          })
        })
      }
    }
  }

  private def updateCache(rate: Either[Error, Rate], pair: Rate.Pair, cache: CaffeineCache[Rate]): F[Error Either Rate] = {
    rate match {
      case Left(_) => Sync[F].pure(rate)
      case Right(r) => for {
        _ <- cache.put(s"${pair.from.show}:${pair.to.show}")(r)
      } yield r.asRight[Error]
    }
  }

  private def updateAndGetFromApi(pair: Rate.Pair, cC: CaffeineCache[Rate]): F[Error Either Rate] = {
    for {
      rate <- fromApi(pair)
      result <- updateCache(rate, pair, cC)
    } yield {
      result
    }
  }

  private def updateIfNotPresent(pair: Rate.Pair, rate: Option[Rate]): F[Error Either Rate] = {
    rate match {
      case Some(e) => Sync[F].pure(Either.right[Error, Rate](e))
      case None => updateAndGetFromApi(pair, caffeineCache)
    }
  }

  def getFromAPI(pair: Rate.Pair): F[Error Either Rate] = {
    for {
      rateFromCache <- caffeineCache.get(s"${pair.from.show}:${pair.to.show}")
      result <- updateIfNotPresent(pair, rateFromCache)
    } yield
      result
  }
}
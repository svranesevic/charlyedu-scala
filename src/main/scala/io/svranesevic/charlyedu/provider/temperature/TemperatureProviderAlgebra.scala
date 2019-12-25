package io.svranesevic.charlyedu.provider.temperature

import java.time.ZonedDateTime

import cats.Parallel
import cats.effect.concurrent.Semaphore
import cats.effect.{ Concurrent, ContextShift }
import cats.implicits._
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import com.softwaremill.sttp.{ Request, SttpBackend, Uri }
import io.svranesevic.charlyedu.util.TimeUtil
import simulacrum.typeclass

import scala.language.higherKinds

@typeclass
trait TemperatureProviderAlgebra[F[_]] {

  import TemperatureProviderAlgebra._

  def forPeriod(inclusiveFrom: ZonedDateTime, inclusiveTo: ZonedDateTime): F[List[Temperature]]
}

object TemperatureProviderAlgebra {

  case class Temperature(temp: Double, date: ZonedDateTime)

  import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
  import io.circe.{ Decoder, Encoder }

  implicit val decoder: Decoder[Temperature] = deriveDecoder
  implicit val encoder: Encoder[Temperature] = deriveEncoder

  def impl[F[_]: Concurrent: Parallel: ContextShift](
      uri: Uri,
      concurrencyLimit: Int
  ): TemperatureProviderAlgebra[F] =
    new TemperatureProviderAlgebra[F] {

      import io.svranesevic.charlyedu.codec.Implicits._
      import tapir._
      import tapir.client.sttp._
      import tapir.json.circe._

      implicit private val backend: SttpBackend[F, Nothing] = AsyncHttpClientCatsBackend[F]()

      private val semaphore = Semaphore[F](concurrencyLimit)

      private val temperatureRequest: ZonedDateTime => Request[Either[String, Temperature], Nothing] =
        endpoint.get
          .in("")
          .in(query[ZonedDateTime]("at"))
          .errorOut(stringBody)
          .out(jsonBody[Temperature])
          .toSttpRequest(uri)

      protected def forDay(day: ZonedDateTime): F[Temperature] =
        for {
          s        <- semaphore
          response <- s.withPermit(temperatureRequest(day).send())

          temperature <- response.body match {
            case Left(cause)        => new Throwable(s"Could not decode response body: $cause").raiseError[F, Temperature]
            case Right(Left(cause)) => new Throwable(s"Could not obtain temperature: $cause").raiseError[F, Temperature]
            case Right(Right(temp)) => temp.pure[F]
          }
        } yield temperature

      override def forPeriod(inclusiveFrom: ZonedDateTime, inclusiveTo: ZonedDateTime): F[List[Temperature]] =
        TimeUtil
          .daysBetween(inclusiveFrom, inclusiveTo)
          .parTraverse(forDay)
    }
}

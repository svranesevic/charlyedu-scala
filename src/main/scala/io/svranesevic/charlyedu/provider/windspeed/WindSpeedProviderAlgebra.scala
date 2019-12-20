package io.svranesevic.charlyedu.provider.windspeed

import java.time.ZonedDateTime

import cats.Parallel
import cats.effect.concurrent.Semaphore
import cats.effect.{ Concurrent, ContextShift }
import cats.implicits._
import com.softwaremill.sttp.{ Request, SttpBackend, Uri }
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import io.svranesevic.charlyedu.util.TimeUtil
import simulacrum.typeclass

import scala.language.higherKinds

@typeclass
trait WindSpeedProviderAlgebra[F[_]] {

  import WindSpeedProviderAlgebra._

  def forPeriod(inclusiveFrom: ZonedDateTime, inclusiveTo: ZonedDateTime): F[List[WindSpeed]]
}

object WindSpeedProviderAlgebra {

  case class WindSpeed(north: Double, west: Double, date: ZonedDateTime)

  import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
  import io.circe.{ Decoder, Encoder }

  implicit val decoder: Decoder[WindSpeed] = deriveDecoder
  implicit val encoder: Encoder[WindSpeed] = deriveEncoder

  def impl[F[_]: Concurrent: Parallel: ContextShift](
      uri: Uri,
      concurrencyLimit: Int
  ): WindSpeedProviderAlgebra[F] = new WindSpeedProviderAlgebra[F] {

    import io.svranesevic.charlyedu.codec.Implicits._
    import tapir._
    import tapir.client.sttp._
    import tapir.json.circe._

    implicit private val backend: SttpBackend[F, Nothing] = AsyncHttpClientCatsBackend[F]()

    private val semaphore = Semaphore[F](concurrencyLimit)

    private val windSpeedRequest: ZonedDateTime => Request[Either[String, WindSpeed], Nothing] =
      endpoint.get
        .in("")
        .in(query[ZonedDateTime]("at"))
        .errorOut(stringBody)
        .out(jsonBody[WindSpeed])
        .toSttpRequest(uri)

    protected def forDay(day: ZonedDateTime): F[WindSpeed] =
      for {
        s        <- semaphore
        response <- s.withPermit(windSpeedRequest.apply(day).send())

        windSpeed <- response.body match {
          case Left(cause)        => new Throwable(s"Could not decode response body: $cause").raiseError[F, WindSpeed]
          case Right(Left(cause)) => new Throwable(s"Could not obtain temperature: $cause").raiseError[F, WindSpeed]
          case Right(Right(temp)) => temp.pure[F]
        }
      } yield windSpeed

    override def forPeriod(inclusiveFrom: ZonedDateTime, inclusiveTo: ZonedDateTime): F[List[WindSpeed]] =
      TimeUtil
        .daysBetween(inclusiveFrom, inclusiveTo)
        .parTraverse(forDay)
  }
}

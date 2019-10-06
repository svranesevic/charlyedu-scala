package io.svranesevic.charlyedu.provider.windspeed

import java.time.{ LocalDate, LocalTime, ZoneId, ZonedDateTime }

import cats.Parallel
import cats.effect.concurrent.Semaphore
import cats.effect.{ Async, Concurrent, ContextShift }
import cats.implicits._
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import com.softwaremill.sttp.{ Request, SttpBackend, Uri }
import io.svranesevic.charlyedu.provider.windspeed.WindSpeedProviderAlgebra._
import io.svranesevic.charlyedu.util.TimeUtil
import tapir._
import tapir.client.sttp._
import tapir.json.circe._

import scala.language.higherKinds

private class WindSpeedProviderInterpreter[F[_]](uri: Uri, semaphore: F[Semaphore[F]])(
    implicit F: Async[F],
    cs: ContextShift[F],
    p: Parallel[F]
) extends WindSpeedProviderAlgebra[F, List] {

  import WindSpeedProviderInterpreter._
  import io.svranesevic.charlyedu.codec.Implicits._

  private val windSpeedRequest: ZonedDateTime => Request[Either[String, WindSpeed], Nothing] =
    endpoint.get
      .in("")
      .in(query[ZonedDateTime]("at"))
      .errorOut(stringBody)
      .out(jsonBody[WindSpeed])
      .toSttpRequest(uri)

  implicit private val backend: SttpBackend[F, Nothing] = AsyncHttpClientCatsBackend[F]()

  protected def forDay(day: ZonedDateTime): F[WindSpeed] =
    for {
      s <- semaphore

      response <- s.withPermit {
        windSpeedRequest
          .apply(day)
          .send()
      }

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

object WindSpeedProviderInterpreter {

  def apply[F[_]: Concurrent: ContextShift: Parallel](
      uri: Uri,
      concurrencyLimit: Int
  ): WindSpeedProviderAlgebra[F, List] =
    new WindSpeedProviderInterpreter[F](uri, Semaphore[F](concurrencyLimit))

  import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
  import io.circe.{ Decoder, Encoder }

  implicit val decoder: Decoder[WindSpeed] = deriveDecoder
  implicit val encoder: Encoder[WindSpeed] = deriveEncoder
}

package io.svranesevic.charlyedu.provider.windspeed

import java.time.{ LocalDate, LocalTime, ZoneId, ZonedDateTime }

import cats.Parallel
import cats.effect.concurrent.Semaphore
import cats.effect.{ Concurrent, ContextShift }
import cats.implicits._
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import com.softwaremill.sttp.{ Request, SttpBackend, Uri }
import io.svranesevic.charlyedu.provider.windspeed.WindSpeedProviderAlgebra._
import tapir._
import tapir.client.sttp._
import tapir.json.circe._

import scala.language.higherKinds

class WindSpeedProviderInterpreter[F[_]](uri: Uri, semaphore: F[Semaphore[F]])(implicit F: Concurrent[F],
                                                                               P: Parallel[F],
                                                                               cs: ContextShift[F])
    extends WindSpeedProviderAlgebra[F, List] {

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

  override def forDay(day: ZonedDateTime): F[WindSpeed] =
    for {
      s <- semaphore

      response <- s.withPermit {
        windSpeedRequest
          .apply(day)
          .send()
      }

      windSpeed <- response.body match {
        case Left(cause)        => F.raiseError[WindSpeed](new Throwable(s"Could not decode response body: $cause"))
        case Right(Left(cause)) => F.raiseError[WindSpeed](new Throwable(s"Could not obtain temperature: $cause"))
        case Right(Right(temp)) => F.pure[WindSpeed](temp)
      }
    } yield windSpeed

  override def forPeriod(inclusiveFrom: ZonedDateTime, inclusiveTo: ZonedDateTime): F[List[WindSpeed]] = {
    val between =
      inclusiveFrom.toLocalDate.toEpochDay
        .until(inclusiveTo.plusDays(1).toLocalDate.toEpochDay)
        .map(LocalDate.ofEpochDay)
        .map(ZonedDateTime.of(_, LocalTime.MIDNIGHT, ZoneId.of("UTC")))
        .toList

    between.parTraverse(forDay)
  }
}

object WindSpeedProviderInterpreter {

  def apply[F[_]: Concurrent: ContextShift: Parallel](uri: Uri, concurrencyLimit: Int) =
    new WindSpeedProviderInterpreter[F](uri, Semaphore[F](concurrencyLimit))

  import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
  import io.circe.{ Decoder, Encoder }

  implicit val decoder: Decoder[WindSpeed] = deriveDecoder
  implicit val encoder: Encoder[WindSpeed] = deriveEncoder
}

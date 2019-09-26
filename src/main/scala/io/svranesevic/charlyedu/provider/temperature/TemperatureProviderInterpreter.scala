package io.svranesevic.charlyedu.provider.temperature

import java.time.{ LocalDate, LocalTime, ZoneId, ZonedDateTime }

import cats.Parallel
import cats.effect.concurrent.Semaphore
import cats.effect.{ Async, Concurrent, ContextShift }
import cats.implicits._
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import com.softwaremill.sttp.{ Request, SttpBackend, Uri }
import io.svranesevic.charlyedu.provider.temperature.TemperatureProviderAlgebra._
import tapir._
import tapir.client.sttp._
import tapir.json.circe._

import scala.language.higherKinds

private class TemperatureProviderInterpreter[F[_]](uri: Uri, semaphore: F[Semaphore[F]])(
    implicit F: Async[F],
    CS: ContextShift[F],
    P: Parallel[F]
) extends TemperatureProviderAlgebra[F, List] {

  import TemperatureProviderInterpreter._
  import io.svranesevic.charlyedu.codec.Implicits._

  private val temperatureRequest: ZonedDateTime => Request[Either[String, Temperature], Nothing] =
    endpoint.get
      .in("")
      .in(query[ZonedDateTime]("at"))
      .errorOut(stringBody)
      .out(jsonBody[Temperature])
      .toSttpRequest(uri)

  implicit private val backend: SttpBackend[F, Nothing] = AsyncHttpClientCatsBackend[F]()

  override def forDay(day: ZonedDateTime): F[Temperature] =
    for {
      s <- semaphore

      response <- s
        .withPermit {
          temperatureRequest.apply(day).send()
        }

      temperature <- response.body match {
        case Left(cause)        => new Throwable(s"Could not decode response body: $cause").raiseError[F, Temperature]
        case Right(Left(cause)) => new Throwable(s"Could not obtain temperature: $cause").raiseError[F, Temperature]
        case Right(Right(temp)) => temp.pure[F]
      }
    } yield temperature

  override def forPeriod(inclusiveFrom: ZonedDateTime, inclusiveTo: ZonedDateTime): F[List[Temperature]] = {
    val between =
      inclusiveFrom.toLocalDate.toEpochDay
        .until(inclusiveTo.plusDays(1).toLocalDate.toEpochDay)
        .map(LocalDate.ofEpochDay)
        .map(ZonedDateTime.of(_, LocalTime.MIDNIGHT, ZoneId.of("UTC")))
        .toList

    between.parTraverse(forDay)
  }
}

object TemperatureProviderInterpreter {

  def apply[F[_]: Concurrent: ContextShift: Parallel](
      uri: Uri,
      concurrencyLimit: Int
  ): TemperatureProviderAlgebra[F, List] =
    new TemperatureProviderInterpreter[F](uri, Semaphore[F](concurrencyLimit))

  import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
  import io.circe.{ Decoder, Encoder }

  implicit val decoder: Decoder[Temperature] = deriveDecoder
  implicit val encoder: Encoder[Temperature] = deriveEncoder
}

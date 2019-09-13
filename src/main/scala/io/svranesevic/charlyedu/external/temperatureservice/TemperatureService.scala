package io.svranesevic.charlyedu.external.temperatureservice

import java.time.{LocalDate, LocalTime, ZoneId, ZonedDateTime}

import cats.effect.{Async, Blocker, ContextShift}
import cats.implicits._
import cats.Parallel
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend._
import com.softwaremill.sttp.{SttpBackend, Uri}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import tapir.client.sttp._
import tapir.json.circe._
import tapir.{endpoint => tapirEndpoint, _}

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

class TemperatureService[F[_]](uri: Uri, blocker: Blocker)(implicit F: Async[F], P: Parallel[F], cs: ContextShift[F]) {

  import io.svranesevic.charlyedu.codec.Implicits._

  implicit private val decoder: Decoder[Temperature] = deriveDecoder
  implicit private val encoder: Encoder[Temperature] = deriveEncoder

  private val endpoint: Endpoint[ZonedDateTime, String, Temperature, Nothing] =
    tapirEndpoint
      .get
      .in("")
      .in(query[ZonedDateTime]("at"))
      .errorOut(stringBody)
      .out(jsonBody[Temperature])

  implicit private val backend: SttpBackend[F, Nothing] = AsyncHttpClientCatsBackend[F]()

  def temperature(at: ZonedDateTime): F[Temperature] = {
    endpoint
      .toSttpRequest(uri)
      .apply(at)
      .send()
      .map(_.body)
      .flatMap {
        case Right(Right(temp: Temperature)) => F.pure(temp)
        case Left(cause) => F.raiseError(new Throwable(s"Could not decode response body: $cause"))
        case Right(Left(cause)) => F.raiseError(new Throwable(s"Could not obtain temperature: $cause"))
      }
  }

  def temperature(from: ZonedDateTime, to: ZonedDateTime): F[List[Temperature]] = {
    val between =
      from.toLocalDate.toEpochDay.until(to.plusDays(1).toLocalDate.toEpochDay)
        .map(LocalDate.ofEpochDay)
        .map(ZonedDateTime.of(_, LocalTime.MIDNIGHT, ZoneId.of("UTC")))
        .toList

    between.parTraverse(at => blocker.blockOn(temperature(at)))
  }
}

object TemperatureService {

  def apply[F[_] : Async : ContextShift : Parallel](uri: Uri, blocker: Blocker)(implicit blockEc: ExecutionContext) = new TemperatureService[F](uri, blocker)
}

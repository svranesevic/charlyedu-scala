package io.svranesevic.charlyedu.external.windspeedservice

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

class WindSpeedService[F[_]](uri: Uri, blocker: Blocker)(implicit F: Async[F], P: Parallel[F], cs: ContextShift[F]) {

  import io.svranesevic.charlyedu.codec.Implicits._

  implicit private val decoder: Decoder[WindSpeed] = deriveDecoder
  implicit private val encoder: Encoder[WindSpeed] = deriveEncoder

  private val endpoint: Endpoint[ZonedDateTime, String, WindSpeed, Nothing] =
    tapirEndpoint
      .get
      .in("")
      .in(query[ZonedDateTime]("at"))
      .errorOut(stringBody)
      .out(jsonBody[WindSpeed])

  implicit private val backend: SttpBackend[F, Nothing] = AsyncHttpClientCatsBackend[F]()

  def windSpeed(at: ZonedDateTime): F[WindSpeed] = {
    endpoint
      .toSttpRequest(uri)
      .apply(at)
      .send()
      .map(_.body)
      .flatMap {
        case Right(Right(ws: WindSpeed)) => F.pure(ws)
        case Left(cause) => F.raiseError(new Throwable(s"Could not decode response body: $cause"))
        case Right(Left(cause)) => F.raiseError(new Throwable(s"Could not obtain wind speed: $cause"))
      }
  }

  def windSpeed(from: ZonedDateTime, to: ZonedDateTime): F[List[WindSpeed]] = {
    val between =
      from.toLocalDate.toEpochDay.until(to.plusDays(1).toLocalDate.toEpochDay)
        .map(LocalDate.ofEpochDay)
        .map(ZonedDateTime.of(_, LocalTime.MIDNIGHT, ZoneId.of("UTC")))
        .toList

    between.parTraverse(at => blocker.blockOn(windSpeed(at)))
  }
}

object WindSpeedService {

  def apply[F[_] : Async : ContextShift : Parallel](uri: Uri, blocker: Blocker)(implicit blockEc: ExecutionContext) = new WindSpeedService[F](uri, blocker)
}

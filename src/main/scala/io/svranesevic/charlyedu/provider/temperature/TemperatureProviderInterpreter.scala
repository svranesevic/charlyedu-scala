package io.svranesevic.charlyedu.provider.temperature

import java.time.{ LocalDate, LocalTime, ZoneId, ZonedDateTime }

import cats.Parallel
import cats.effect.concurrent.Semaphore
import cats.effect.{ Blocker, Concurrent, ContextShift }
import cats.implicits._
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend._
import com.softwaremill.sttp.{ Request, SttpBackend, Uri }
import TemperatureProviderAlgebra._
import tapir._
import tapir.client.sttp._
import tapir.json.circe._

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

class TemperatureProviderInterpreter[F[_]](uri: Uri, semaphore: F[Semaphore[F]], b: Blocker)(implicit F: Concurrent[F],
                                                                                             P: Parallel[F],
                                                                                             cs: ContextShift[F])
    extends TemperatureProviderAlgebra[F, List] {

  import io.svranesevic.charlyedu.codec.Implicits._
  import TemperatureProviderInterpreter._

  private val temperatureRequest: ZonedDateTime => Request[Either[String, Temperature], Nothing] =
    endpoint.get
      .in("")
      .in(query[ZonedDateTime]("at"))
      .errorOut(stringBody)
      .out(jsonBody[Temperature])
      .toSttpRequest(uri)

  implicit private val backend: SttpBackend[F, Nothing] = AsyncHttpClientCatsBackend[F]()

  def forDay(at: ZonedDateTime): F[Temperature] =
    temperatureRequest
      .apply(at)
      .send()
      .map(_.body)
      .flatMap {
        case Right(Right(temp: Temperature)) => F.pure(temp)
        case Left(cause)                     => F.raiseError(new Throwable(s"Could not decode response body: $cause"))
        case Right(Left(cause))              => F.raiseError(new Throwable(s"Could not obtain temperature: $cause"))
      }

  def forPeriod(inclusiveFrom: ZonedDateTime, inclusiveTo: ZonedDateTime): F[List[Temperature]] = {
    val between =
      inclusiveFrom.toLocalDate.toEpochDay
        .until(inclusiveTo.plusDays(1).toLocalDate.toEpochDay)
        .map(LocalDate.ofEpochDay)
        .map(ZonedDateTime.of(_, LocalTime.MIDNIGHT, ZoneId.of("UTC")))
        .toList

    semaphore.flatMap { s =>
      between.parTraverse(at => s.withPermit(b.blockOn(forDay(at))))
    }
  }
}

object TemperatureProviderInterpreter {

  def apply[F[_]: Concurrent: ContextShift: Parallel](uri: Uri, concurrencyLimit: Int, b: Blocker)(
      implicit blockEc: ExecutionContext
  ) =
    new TemperatureProviderInterpreter[F](uri, Semaphore[F](concurrencyLimit), b)

  import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
  import io.circe.{ Decoder, Encoder }

  implicit val decoder: Decoder[Temperature] = deriveDecoder
  implicit val encoder: Encoder[Temperature] = deriveEncoder
}

package io.svranesevic.charlyedu.provider.temperature

import java.time.{ LocalDate, LocalTime, ZoneId, ZonedDateTime }

import com.softwaremill.sttp.asynchttpclient.monix.AsyncHttpClientMonixBackend
import com.softwaremill.sttp.{ Request, SttpBackend, Uri }
import io.svranesevic.charlyedu.provider.temperature.TemperatureProviderAlgebra._
import monix.catnap.Semaphore
import monix.eval.Task
import tapir._
import tapir.client.sttp._
import tapir.json.circe._

import scala.language.higherKinds

class TemperatureProviderInterpreter(uri: Uri, semaphore: Task[Semaphore[Task]])
    extends TemperatureProviderAlgebra[Task, List] {

  import TemperatureProviderInterpreter._
  import io.svranesevic.charlyedu.codec.Implicits._

  private val temperatureRequest: ZonedDateTime => Request[Either[String, Temperature], Nothing] =
    endpoint.get
      .in("")
      .in(query[ZonedDateTime]("at"))
      .errorOut(stringBody)
      .out(jsonBody[Temperature])
      .toSttpRequest(uri)

  implicit private val backend: SttpBackend[Task, Nothing] = AsyncHttpClientMonixBackend()

  def forDay(at: ZonedDateTime): Task[Temperature] =
    for {
      s <- semaphore

      response <- s.withPermit {
        temperatureRequest
          .apply(at)
          .send()
      }

      temperature <- response.body match {
        case Left(cause)        => Task.raiseError[Temperature](new Throwable(s"Could not decode response body: $cause"))
        case Right(Left(cause)) => Task.raiseError[Temperature](new Throwable(s"Could not obtain temperature: $cause"))
        case Right(Right(temp)) => Task.pure[Temperature](temp)
      }
    } yield temperature

  def forPeriod(inclusiveFrom: ZonedDateTime, inclusiveTo: ZonedDateTime): Task[List[Temperature]] = {
    val days =
      inclusiveFrom.toLocalDate.toEpochDay
        .until(inclusiveTo.plusDays(1).toLocalDate.toEpochDay)
        .map(LocalDate.ofEpochDay)
        .map(ZonedDateTime.of(_, LocalTime.MIDNIGHT, ZoneId.of("UTC")))
        .toList

    for {
      temperatures <- Task.wander(days)(forDay)
    } yield temperatures.sortBy(_.date.toEpochSecond)
  }
}

object TemperatureProviderInterpreter {

  def apply(uri: Uri, concurrencyLimit: Int) =
    new TemperatureProviderInterpreter(uri, Semaphore[Task](concurrencyLimit))

  import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
  import io.circe.{ Decoder, Encoder }

  implicit val decoder: Decoder[Temperature] = deriveDecoder
  implicit val encoder: Encoder[Temperature] = deriveEncoder
}

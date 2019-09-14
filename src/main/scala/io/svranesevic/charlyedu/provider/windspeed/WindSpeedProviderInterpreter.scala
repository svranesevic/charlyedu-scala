package io.svranesevic.charlyedu.provider.windspeed

import java.time.{ LocalDate, LocalTime, ZoneId, ZonedDateTime }

import com.softwaremill.sttp.asynchttpclient.monix.AsyncHttpClientMonixBackend
import com.softwaremill.sttp.{ Request, SttpBackend, Uri }
import io.svranesevic.charlyedu.provider.windspeed.WindSpeedProviderAlgebra._
import monix.catnap.Semaphore
import monix.eval.Task
import tapir._
import tapir.client.sttp._
import tapir.json.circe._

import scala.language.higherKinds

class WindSpeedProviderInterpreter(uri: Uri, semaphore: Task[Semaphore[Task]])
    extends WindSpeedProviderAlgebra[Task, List] {

  import WindSpeedProviderInterpreter._
  import io.svranesevic.charlyedu.codec.Implicits._

  private val windSpeedRequest: ZonedDateTime => Request[Either[String, WindSpeed], Nothing] =
    endpoint.get
      .in("")
      .in(query[ZonedDateTime]("at"))
      .errorOut(stringBody)
      .out(jsonBody[WindSpeed])
      .toSttpRequest(uri)

  implicit private val backend: SttpBackend[Task, Nothing] = AsyncHttpClientMonixBackend()

  def forDay(day: ZonedDateTime): Task[WindSpeed] =
    for {
      s <- semaphore

      response <- s.withPermit {
        windSpeedRequest
          .apply(day)
          .send()
      }

      windSpeed <- response.body match {
        case Left(cause)        => Task.raiseError[WindSpeed](new Throwable(s"Could not decode response body: $cause"))
        case Right(Left(cause)) => Task.raiseError[WindSpeed](new Throwable(s"Could not obtain temperature: $cause"))
        case Right(Right(temp)) => Task.pure[WindSpeed](temp)
      }
    } yield windSpeed

  def forPeriod(inclusiveFrom: ZonedDateTime, inclusiveTo: ZonedDateTime): Task[List[WindSpeed]] = {
    val days =
      inclusiveFrom.toLocalDate.toEpochDay
        .until(inclusiveTo.plusDays(1).toLocalDate.toEpochDay)
        .map(LocalDate.ofEpochDay)
        .map(ZonedDateTime.of(_, LocalTime.MIDNIGHT, ZoneId.of("UTC")))
        .toList

    for {
      windSpeeds <- Task.wander(days)(forDay)
    } yield windSpeeds.sortBy(_.date.toEpochSecond)
  }
}

object WindSpeedProviderInterpreter {

  def apply(uri: Uri, concurrencyLimit: Int) =
    new WindSpeedProviderInterpreter(uri, Semaphore[Task](concurrencyLimit))

  import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
  import io.circe.{ Decoder, Encoder }

  implicit val decoder: Decoder[WindSpeed] = deriveDecoder
  implicit val encoder: Encoder[WindSpeed] = deriveEncoder
}

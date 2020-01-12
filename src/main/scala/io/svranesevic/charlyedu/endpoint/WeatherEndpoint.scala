package io.svranesevic.charlyedu.endpoint

import java.time.ZonedDateTime

import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.{ Decoder, Encoder }
import tapir.json.circe._
import tapir.{ endpoint => tapirEndpoint, _ }

object WeatherEndpoint {

  case class Weather(north: Double, west: Double, temp: Double, date: ZonedDateTime)

  implicit val decoder: Decoder[Weather] = deriveDecoder
  implicit val encoder: Encoder[Weather] = deriveEncoder

  val endpoint: Endpoint[(ZonedDateTime, ZonedDateTime), (Int, ErrorResponse), List[Weather], Nothing] =
    tapirEndpoint.get
      .in("weather")
      .in(startDateParameter)
      .in(endDateParameter)
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .out(jsonBody[List[Weather]])
}

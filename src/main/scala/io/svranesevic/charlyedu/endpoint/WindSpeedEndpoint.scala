package io.svranesevic.charlyedu.endpoint

import java.time.ZonedDateTime

import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.{ Decoder, Encoder }
import tapir.json.circe._
import tapir.{ endpoint => tapirEndpoint, _ }

object WindSpeedEndpoint {

  case class WindSpeed(north: Double, west: Double, date: ZonedDateTime)

  implicit val decoder: Decoder[WindSpeed] = deriveDecoder
  implicit val encoder: Encoder[WindSpeed] = deriveEncoder

  val endpoint: Endpoint[(ZonedDateTime, ZonedDateTime), (Int, ErrorResponse), List[WindSpeed], Nothing] =
    tapirEndpoint.get
      .in("speeds")
      .in(startDateParameter)
      .in(endDateParameter)
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .out(jsonBody[List[WindSpeed]])
}

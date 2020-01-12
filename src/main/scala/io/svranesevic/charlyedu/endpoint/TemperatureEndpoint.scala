package io.svranesevic.charlyedu.endpoint

import java.time.ZonedDateTime

import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.{ Decoder, Encoder }
import tapir.json.circe._
import tapir.{ endpoint => tapirEndpoint, _ }

object TemperatureEndpoint {

  case class Temperature(temp: Double, date: ZonedDateTime)

  implicit val decoder: Decoder[Temperature] = deriveDecoder
  implicit val encoder: Encoder[Temperature] = deriveEncoder

  val endpoint: Endpoint[(ZonedDateTime, ZonedDateTime), (Int, ErrorResponse), List[Temperature], Nothing] =
    tapirEndpoint.get
      .in("temperatures")
      .in(startDateParameter)
      .in(endDateParameter)
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .out(jsonBody[List[Temperature]])
}

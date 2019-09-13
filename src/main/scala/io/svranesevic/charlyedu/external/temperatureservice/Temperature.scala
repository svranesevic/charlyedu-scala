package io.svranesevic.charlyedu.external.temperatureservice

import java.time.OffsetDateTime

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class Temperature(temp: Double, date: OffsetDateTime)

object Temperature {

  implicit val decoder: Decoder[Temperature] = deriveDecoder
  implicit val encoder: Encoder[Temperature] = deriveEncoder
}

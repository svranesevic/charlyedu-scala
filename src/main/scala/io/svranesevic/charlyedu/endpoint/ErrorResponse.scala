package io.svranesevic.charlyedu.endpoint

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class ErrorResponse(description: String)

object ErrorResponse {

  implicit val decoder: Decoder[ErrorResponse] = deriveDecoder
  implicit val encoder: Encoder[ErrorResponse] = deriveEncoder

}

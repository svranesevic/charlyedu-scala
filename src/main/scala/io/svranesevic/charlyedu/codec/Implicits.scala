package io.svranesevic.charlyedu.codec

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import tapir.{Codec, MediaType}

object Implicits {

  implicit val zonedDateTimeCodec: Codec[ZonedDateTime, MediaType.TextPlain, String] =
    Codec.stringPlainCodecUtf8.map[ZonedDateTime](ZonedDateTime.parse(_))(_.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")))
}

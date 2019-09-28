package io.svranesevic.charlyedu.codec

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import io.svranesevic.charlyedu.util.TimeUtil
import tapir.{ Codec, MediaType }

object Implicits {

  implicit val zonedDateTimeCodec: Codec[ZonedDateTime, MediaType.TextPlain, String] =
    Codec.stringPlainCodecUtf8
      .map[ZonedDateTime](str => TimeUtil.limitToNow(ZonedDateTime.parse(str)))(
        _.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))
      )
}

package io.svranesevic.charlyedu

import java.time.{ ZoneId, ZonedDateTime }

import tapir.{ query, EndpointInput, Validator }

package object endpoint {

  import io.svranesevic.charlyedu.codec.Implicits._

  val startDateParameter: EndpointInput.Query[ZonedDateTime] =
    query[ZonedDateTime]("start")
      .validate(
        Validator.custom(time => time.isBefore(ZonedDateTime.now(ZoneId.of("UTC"))),
                         "Date time must not be in the future")
      )

  val endDateParameter: EndpointInput.Query[ZonedDateTime] =
    query[ZonedDateTime]("end")
      .validate(
        Validator.custom(time => time.isBefore(ZonedDateTime.now(ZoneId.of("UTC"))),
                         "Date time must not be in the future")
      )
}

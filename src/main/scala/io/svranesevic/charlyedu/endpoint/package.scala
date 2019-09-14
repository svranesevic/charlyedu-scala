package io.svranesevic.charlyedu

import java.time.ZonedDateTime

import tapir.{ query, EndpointInput }

package object endpoint {

  import io.svranesevic.charlyedu.codec.Implicits._

  val startDateParameter: EndpointInput.Query[ZonedDateTime] = query[ZonedDateTime]("start")
  val endDateParameter: EndpointInput.Query[ZonedDateTime]   = query[ZonedDateTime]("end")
}

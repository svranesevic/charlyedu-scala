package io.svranesevic.charlyedu

import com.softwaremill.sttp.Uri
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.module.sttp._
import pureconfig.generic.ProductHint

case class Config(port: Int, temperatureService: Uri, windSpeedService: Uri)

object Config {

  implicit def camelCaseHint[T]: ProductHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  def build: Config = ConfigSource.default.loadOrThrow[Config]
}

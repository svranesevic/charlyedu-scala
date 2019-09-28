package io.svranesevic.charlyedu.provider.temperature

import java.time.ZonedDateTime

import scala.language.higherKinds

trait TemperatureProviderAlgebra[F[_], G[_]] {

  import TemperatureProviderAlgebra._

  def forPeriod(inclusiveFrom: ZonedDateTime, inclusiveTo: ZonedDateTime): F[G[Temperature]]
}

object TemperatureProviderAlgebra {

  case class Temperature(temp: Double, date: ZonedDateTime)
}

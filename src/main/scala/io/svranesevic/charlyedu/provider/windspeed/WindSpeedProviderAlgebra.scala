package io.svranesevic.charlyedu.provider.windspeed

import java.time.ZonedDateTime

import scala.language.higherKinds

trait WindSpeedProviderAlgebra[F[_], G[_]] {

  import WindSpeedProviderAlgebra._

  def forPeriod(inclusiveFrom: ZonedDateTime, inclusiveTo: ZonedDateTime): F[G[WindSpeed]]
}

object WindSpeedProviderAlgebra {

  case class WindSpeed(north: Double, west: Double, date: ZonedDateTime)
}

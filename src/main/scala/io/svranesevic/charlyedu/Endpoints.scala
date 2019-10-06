package io.svranesevic.charlyedu

import java.time.ZonedDateTime

import cats.effect.Sync
import cats.implicits._
import cats.{ Foldable, Monad }
import io.scalaland.chimney.dsl._
import io.svranesevic.charlyedu.endpoint.WeatherEndpoint.Weather
import io.svranesevic.charlyedu.endpoint.{ ErrorResponse, TemperatureEndpoint, WeatherEndpoint, WindSpeedEndpoint }
import io.svranesevic.charlyedu.provider.temperature.TemperatureProviderAlgebra
import io.svranesevic.charlyedu.provider.windspeed.WindSpeedProviderAlgebra
import tapir.server.ServerEndpoint

import scala.language.higherKinds

class Endpoints[F[_]: Sync, G[_]: Monad: Foldable](
    temperatureProvider: TemperatureProviderAlgebra[F, G],
    windsSpeedProvider: WindSpeedProviderAlgebra[F, G]
) {
  val temperatureEndpoint: ServerEndpoint[(ZonedDateTime, ZonedDateTime), ErrorResponse, List[
    TemperatureEndpoint.Temperature
  ], Nothing, F] =
    TemperatureEndpoint.endpoint.serverLogic {
      case (from, to) =>
        for {
          temperatures <- temperatureProvider.forPeriod(from, to)
          dto = temperatures
            .map(_.into[TemperatureEndpoint.Temperature].transform)
            .foldLeft(List[TemperatureEndpoint.Temperature]())(_ :+ _)
        } yield dto.asRight[ErrorResponse]
    }

  val windSpeedEndpoint
    : ServerEndpoint[(ZonedDateTime, ZonedDateTime), ErrorResponse, List[WindSpeedEndpoint.WindSpeed], Nothing, F] =
    WindSpeedEndpoint.endpoint.serverLogic {
      case (from, to) =>
        for {
          windSpeeds <- windsSpeedProvider.forPeriod(from, to)
          dto = windSpeeds
            .map(_.into[WindSpeedEndpoint.WindSpeed].transform)
            .foldLeft(List[WindSpeedEndpoint.WindSpeed]())(_ :+ _)
        } yield dto.asRight[ErrorResponse]
    }

  val weatherEndpoint: ServerEndpoint[(ZonedDateTime, ZonedDateTime), ErrorResponse, List[Weather], Nothing, F] =
    WeatherEndpoint.endpoint.serverLogic {
      case (from, to) =>
        for {
          windSpeeds   <- windsSpeedProvider.forPeriod(from, to)
          temperatures <- temperatureProvider.forPeriod(from, to)

          dto = (windSpeeds, temperatures)
            .mapN { (windSpeed, temperature) =>
              Weather(windSpeed.north, windSpeed.west, temperature.temp, windSpeed.date)
            }
            .foldLeft(List[WeatherEndpoint.Weather]())(_ :+ _)
        } yield dto.asRight[ErrorResponse]
    }

  val all = List(temperatureEndpoint, windSpeedEndpoint, weatherEndpoint)
}

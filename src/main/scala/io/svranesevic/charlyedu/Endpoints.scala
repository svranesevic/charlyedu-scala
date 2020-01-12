package io.svranesevic.charlyedu

import java.time.ZonedDateTime

import cats.effect.Concurrent
import cats.syntax.all._
import io.scalaland.chimney.dsl._
import io.svranesevic.charlyedu.endpoint.WeatherEndpoint.Weather
import io.svranesevic.charlyedu.endpoint.{ ErrorResponse, TemperatureEndpoint, WeatherEndpoint, WindSpeedEndpoint }
import io.svranesevic.charlyedu.provider.temperature.TemperatureProviderAlgebra
import io.svranesevic.charlyedu.provider.windspeed.WindSpeedProviderAlgebra
import io.svranesevic.charlyedu.util.Syntax._
import tapir.model.StatusCodes
import tapir.server.ServerEndpoint

import scala.concurrent.TimeoutException
import scala.language.higherKinds

case class Endpoints[F[_]: Concurrent](
    temperatureProvider: TemperatureProviderAlgebra[F],
    windsSpeedProvider: WindSpeedProviderAlgebra[F]
) {

  import cats.instances.list._

  val temperatureEndpoint: ServerEndpoint[(ZonedDateTime, ZonedDateTime), (Int, ErrorResponse), List[
    TemperatureEndpoint.Temperature
  ], Nothing, F] =
    TemperatureEndpoint.endpoint.serverLogic {
      case (from, to) =>
        (for {
          temperatures <- temperatureProvider.forPeriod(from, to)
          dto = temperatures
            .map(_.into[TemperatureEndpoint.Temperature].transform)
            .foldLeft(List[TemperatureEndpoint.Temperature]())(_ :+ _)
        } yield dto.asRight[(Int, ErrorResponse)]).handleError {
          case to: TimeoutException => (StatusCodes.RequestTimeout, ErrorResponse(to.getMessage)).asLeft
        }
    }

  val windSpeedEndpoint: ServerEndpoint[(ZonedDateTime, ZonedDateTime), (Int, ErrorResponse), List[
    WindSpeedEndpoint.WindSpeed
  ], Nothing, F] =
    WindSpeedEndpoint.endpoint.serverLogic {
      case (from, to) =>
        (for {
          windSpeeds <- windsSpeedProvider.forPeriod(from, to)
          dto = windSpeeds
            .map(_.into[WindSpeedEndpoint.WindSpeed].transform)
            .foldLeft(List[WindSpeedEndpoint.WindSpeed]())(_ :+ _)
        } yield dto.asRight[(Int, ErrorResponse)]).handleError {
          case to: TimeoutException => (StatusCodes.RequestTimeout, ErrorResponse(to.getMessage)).asLeft
        }
    }

  val weatherEndpoint: ServerEndpoint[(ZonedDateTime, ZonedDateTime), (Int, ErrorResponse), List[Weather], Nothing, F] =
    WeatherEndpoint.endpoint.serverLogic {
      case (from, to) =>
        (for {
          windSpeedsF   <- windsSpeedProvider.forPeriod(from, to).start
          temperaturesF <- temperatureProvider.forPeriod(from, to).start

          windSpeeds   <- windSpeedsF.join
          temperatures <- temperaturesF.join

          dto = (windSpeeds, temperatures)
            .mapN { (windSpeed, temperature) =>
              Weather(windSpeed.north, windSpeed.west, temperature.temp, windSpeed.date)
            }
            .foldLeft(List[WeatherEndpoint.Weather]())(_ :+ _)
        } yield dto.asRight[(Int, ErrorResponse)]).handleError {
          case to: TimeoutException => (StatusCodes.RequestTimeout, ErrorResponse(to.getMessage)).asLeft
        }
    }

  val all = List(temperatureEndpoint, windSpeedEndpoint, weatherEndpoint)
}

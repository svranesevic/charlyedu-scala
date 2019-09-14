package io.svranesevic.charlyedu

import cats.effect._
import cats.implicits._
import io.scalaland.chimney.dsl._
import io.svranesevic.charlyedu.endpoint.{ ErrorResponse, TemperatureEndpoint, WindSpeedEndpoint }
import io.svranesevic.charlyedu.provider.temperature.TemperatureProviderInterpreter
import io.svranesevic.charlyedu.provider.windspeed.WindSpeedProviderInterpreter
import monix.eval.{ Task, TaskApp }
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import tapir.server.http4s._

object Server extends TaskApp {

  private val config = Config.build

  private val windsSpeedProvider =
    WindSpeedProviderInterpreter(config.windSpeedService, 10000)

  private val temperatureProvider =
    TemperatureProviderInterpreter(config.temperatureService, 10000)

  private val temperatureRoutes = TemperatureEndpoint.endpoint.toRoutes {
    case (from, to) =>
      for {
        temperatures <- temperatureProvider.forPeriod(from, to)
        dto = temperatures.map(_.into[TemperatureEndpoint.Temperature].transform)
      } yield dto.asRight[ErrorResponse]
  }

  private val windSpeedRoutes = WindSpeedEndpoint.endpoint.toRoutes {
    case (from, to) =>
      for {
        windSpeeds <- windsSpeedProvider.forPeriod(from, to)
        dto = windSpeeds.map(_.into[WindSpeedEndpoint.WindSpeed].transform)
      } yield dto.asRight[ErrorResponse]
  }

  private val router = Router("/" -> temperatureRoutes, "/" -> windSpeedRoutes).orNotFound

  override def run(args: List[String]): Task[ExitCode] =
    BlazeServerBuilder[Task]
      .bindHttp(config.port, "0.0.0.0")
      .withHttpApp(router)
      .resource
      .use(_ => Task.never)
      .as(ExitCode.Success)
}

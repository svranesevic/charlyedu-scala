package io.svranesevic.charlyedu

import java.util.concurrent.Executors

import cats.effect.{IO, _}
import cats.implicits._
import io.scalaland.chimney.dsl._
import io.svranesevic.charlyedu.endpoint.{ErrorResponse, TemperatureEndpoint, WindSpeedEndpoint}
import io.svranesevic.charlyedu.external.temperatureservice.TemperatureService
import io.svranesevic.charlyedu.external.windspeedservice.WindSpeedService
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import tapir.server.http4s._

import scala.concurrent.ExecutionContext

object Server extends IOApp {

  implicit val blockingEc: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  private val config = Config.build

  private val windsSpeedService = WindSpeedService[IO](config.windSpeedService, Blocker.liftExecutionContext(blockingEc))
  private val temperatureService = TemperatureService[IO](config.temperatureService, Blocker.liftExecutionContext(blockingEc))

  private val temperatureRoutes = TemperatureEndpoint.endpoint.toRoutes {
    case (from, to) =>
      temperatureService.temperature(from, to)
        .map(_.map(_.into[TemperatureEndpoint.Temperature].transform))
        .map(_.asRight[ErrorResponse])
  }

  private val windSpeedRoutes = WindSpeedEndpoint.endpoint.toRoutes {
    case (from, to) =>
      windsSpeedService.windSpeed(from, to)
        .map(_.map(_.into[WindSpeedEndpoint.WindSpeed].transform))
        .map(_.asRight[ErrorResponse])
  }

  private val router = Router("/" -> temperatureRoutes, "/" -> windSpeedRoutes).orNotFound

  override def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(config.port, "localhost")
      .withHttpApp(router)
      .resource
      .use(_ => IO.never)
      .as(ExitCode.Success)
}

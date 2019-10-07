package io.svranesevic.charlyedu

import cats.effect.{ ExitCode, IO, IOApp }
import cats.implicits._
import io.svranesevic.charlyedu.provider.temperature.TemperatureProviderInterpreter
import io.svranesevic.charlyedu.provider.windspeed.WindSpeedProviderInterpreter
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import tapir.docs.openapi._
import tapir.openapi.circe.yaml._
import tapir.server.http4s._
import tapir.swagger.http4s._

object Server extends IOApp {

  private val config = Config.build

  private val windsSpeedProvider =
    WindSpeedProviderInterpreter[IO](config.windSpeedService, 10000)

  private val temperatureProvider =
    TemperatureProviderInterpreter[IO](config.temperatureService, 10000)

  private val endpoints = Endpoints(temperatureProvider, windsSpeedProvider)
  private val docsAsYaml =
    endpoints.all.toOpenAPI("The Service", "1.0").toYaml

  private val router = Router(
    "/"     -> endpoints.all.toRoutes,
    "/docs" -> new SwaggerHttp4s(docsAsYaml).routes
  ).orNotFound

  override def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(config.port, "0.0.0.0")
      .withHttpApp(router)
      .resource
      .use(_ => IO.never)
      .as(ExitCode.Success)
}

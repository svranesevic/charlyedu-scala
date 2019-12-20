val Http4sVersion         = "0.20.15"
val TapirVersion          = "0.11.9"
val SttpVersion           = "1.7.2"
val CirceVersion          = "0.12.3"
val PureConfigVersion     = "0.12.1"
val CatsEffectVersion     = "2.0.0"
val ChimneyVersion        = "0.3.5"
val LogbackClassicVersion = "1.2.3"
val SimulacrumVersion     = "1.0.0"
val ScalaTestVersion      = "3.1.0"

val MacroParadiseVersion = "2.1.1"

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(dockerSettings)
  .settings(
    name := "charlyedu",
    version := "0.1",
    scalaVersion := "2.12.10",
    libraryDependencies ++=
      Seq(
        "org.http4s"             %% "http4s-blaze-server"            % Http4sVersion,
        "org.http4s"             %% "http4s-blaze-client"            % Http4sVersion,
        "org.http4s"             %% "http4s-circe"                   % Http4sVersion,
        "org.http4s"             %% "http4s-dsl"                     % Http4sVersion,
        "com.softwaremill.tapir" %% "tapir-http4s-server"            % TapirVersion,
        "com.softwaremill.tapir" %% "tapir-core"                     % TapirVersion,
        "com.softwaremill.tapir" %% "tapir-json-circe"               % TapirVersion,
        "com.softwaremill.tapir" %% "tapir-sttp-client"              % TapirVersion,
        "com.softwaremill.tapir" %% "tapir-openapi-docs"             % TapirVersion,
        "com.softwaremill.tapir" %% "tapir-openapi-circe-yaml"       % TapirVersion,
        "com.softwaremill.tapir" %% "tapir-swagger-ui-http4s"        % TapirVersion,
        "com.softwaremill.sttp"  %% "core"                           % SttpVersion,
        "com.softwaremill.sttp"  %% "async-http-client-backend-cats" % SttpVersion,
        "io.circe"               %% "circe-core"                     % CirceVersion,
        "io.circe"               %% "circe-generic"                  % CirceVersion,
        "io.circe"               %% "circe-parser"                   % CirceVersion,
        "com.github.pureconfig"  %% "pureconfig"                     % PureConfigVersion,
        "com.github.pureconfig"  %% "pureconfig-sttp"                % PureConfigVersion,
        "org.typelevel"          %% "cats-effect"                    % CatsEffectVersion,
        "io.scalaland"           %% "chimney"                        % ChimneyVersion,
        "ch.qos.logback"         % "logback-classic"                 % LogbackClassicVersion,
        "org.typelevel"          %% "simulacrum"                     % SimulacrumVersion,
        "org.scalatest"          %% "scalatest"                      % ScalaTestVersion % Test
      ),
    libraryDependencies += compilerPlugin(
      "org.scalamacros" % "paradise" % MacroParadiseVersion cross CrossVersion.full
    ),
    scalacOptions ++= Seq("-Ypartial-unification")
  )

lazy val dockerSettings =
  Seq(
    packageName in Docker := "theservice",
    dockerExposedPorts ++= Seq(3000),
    version in Docker := "0.1.0-RC",
    dockerBaseImage := "openjdk:8-slim",
    packageSummary := "The service",
    packageDescription := "",
    dockerUpdateLatest := true,
    publishArtifact := false,
    mainClass in Compile := Some("io.svranesevic.charlyedu.Server"),
    javaOptions in Universal ++= Seq(
      // -J params will be added as jvm parameters
      "-J-Xmx2048m",
      "-J-Xms512m",
      "-J-server"
    ),
    publishTo := Some(Resolver.file("devnull", file("/dev/null")))
  )

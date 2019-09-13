lazy val root = (project in file("."))
  .settings(
    name := "charlyedu",
    version := "0.1",
    scalaVersion := "2.12.8",
    libraryDependencies ++=
      Seq(
        "org.http4s" %% "http4s-blaze-server" % "0.20.0",
        "org.http4s" %% "http4s-blaze-client" % "0.20.0",
        "org.http4s" %% "http4s-circe" % "0.20.0",
        "org.http4s" %% "http4s-dsl" % "0.20.0",
        "io.monix" %% "monix" % "3.0.0",
        "org.typelevel" %% "cats-effect" % "2.0.0",
        "com.softwaremill.tapir" %% "tapir-http4s-server" % "0.11.0",
        "com.softwaremill.tapir" %% "tapir-core" % "0.11.0",
        "com.softwaremill.tapir" %% "tapir-json-circe" % "0.11.0",
        "com.softwaremill.tapir" %% "tapir-sttp-client" % "0.11.0",
        "com.softwaremill.sttp" %% "core" % "1.6.6",
        "com.softwaremill.sttp" %% "async-http-client-backend-cats" % "1.6.6",
        "io.circe" %% "circe-core" % "0.11.1",
        "io.circe" %% "circe-generic" % "0.11.1",
        "io.circe" %% "circe-parser" % "0.11.1",
        "io.scalaland" %% "chimney" % "0.3.2",
        "com.github.pureconfig" %% "pureconfig" % "0.12.0",
        "com.github.pureconfig" %% "pureconfig-sttp" % "0.12.0",
        "ch.qos.logback" % "logback-classic" % "1.2.3"
      ),
    scalacOptions += "-Ypartial-unification"
  )

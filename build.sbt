ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "emnify"
  )

lazy val tapir = Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-core",
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server",
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe",
  "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server",
  "com.softwaremill.sttp.tapir" %% "tapir-cats"
).map(_ % Versions.tapir)

lazy val asyncSttpClient = "com.softwaremill.sttp.client3" %% "async-http-client-backend-fs2" % Versions.sttpVersion

lazy val doobie = Seq(
  "org.tpolecat" %% "doobie-core",
  "org.tpolecat" %% "doobie-h2"
).map(_ % Versions.doobie)

lazy val flyway = "org.flywaydb" % "flyway-core" % Versions.flyway

lazy val h2database = "com.h2database" % "h2" % Versions.h2Database

lazy val http4s = "org.http4s" %% "http4s-blaze-server" % Versions.http4s
lazy val softwaremillCommon = "com.softwaremill.common" %% "tagging" % Versions.softwaremillCommonTagging
lazy val swaggerUI = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % Versions.swaggerUI
lazy val pureConfig = "com.github.pureconfig" %% "pureconfig" % Versions.pureConfig

lazy val logging = Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.11",
  "io.laserdisc" %% "log-effect-fs2" % Versions.logEffectVersion
)

lazy val cats = Seq(
  "org.typelevel" %% "cats-core" % Versions.cats,
  "org.typelevel" %% "cats-effect" % Versions.catsEffect
)

lazy val testingLibs = Seq(
  "org.scalatest" %% "scalatest" % Versions.scalaTest % Test,
  "org.scalameta" %% "munit" % Versions.munitVersion % Test,
  "org.typelevel" %% "munit-cats-effect-3" % Versions.munitCatsEffectVersion % Test
)

lazy val fs2 = Seq(
  "co.fs2" %% "fs2-core" % Versions.fs2,
  "co.fs2" %% "fs2-io" % Versions.fs2
)

libraryDependencies ++=
  cats ++ testingLibs ++
    fs2 ++ tapir ++ doobie ++ logging :+ flyway :+ h2database :+
    pureConfig :+ http4s :+ softwaremillCommon :+ asyncSttpClient :+ swaggerUI

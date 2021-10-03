ThisBuild / scalaVersion := "3.1.0-RC2"
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / githubWorkflowPublishTargetBranches := Seq() // Don't publish anywhere

val Versions =
  new {
    val tapir = "0.19.0-M9"
    val http4s = "0.23.3"
    val logback = "1.2.4"
  }

val commonSettings = Seq(
    scalacOptions -= "-Xfatal-warnings",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-core" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % Versions.tapir,
      "org.typelevel" %% "cats-effect" % "3.2.9",
      "org.typelevel" %% "cats-mtl" % "1.2.1",
      "org.typelevel" %% "munit-cats-effect-3" % "1.0.6" % Test,
      ),
  )

val shared = project.settings(commonSettings)

val server = project.settings(
  commonSettings,
  libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-dsl" % Versions.http4s,
      "org.http4s" %% "http4s-ember-server" % Versions.http4s,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % Versions.tapir,
      "ch.qos.logback" % "logback-classic" % Versions.logback,
    ),
  )
  .dependsOn(shared)

val client = project.settings(
  commonSettings,
  libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-client" % Versions.http4s,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-client" % Versions.tapir,
      "ch.qos.logback" % "logback-classic" % Versions.logback,
    ),
  )
  .dependsOn(shared)

val root = project
  .in(file("."))
  .settings(publish := {}, publish / skip := true)
  .aggregate(server, client, shared)


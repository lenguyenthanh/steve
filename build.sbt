val Versions =
  new {
    val tapir = "0.19.0-M9"
    val http4s = "0.23.3"
    val logback = "1.2.4"
  }

val commonSettings = Seq(
    scalaVersion := "3.1.0-RC2",
    scalacOptions -= "-Xfatal-warnings",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-core" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % Versions.tapir,
      "org.typelevel" %% "cats-effect" % "3.2.9",
      "org.typelevel" %% "cats-mtl" % "1.2.1",
      )
  )

val shared = project.settings(commonSettings)

val server = project.settings(commonSettings)
              .dependsOn(shared)

val client = project.settings(commonSettings)
              .dependsOn(shared)

val root = project
  .in(file("."))
  .settings(publish := {})
  .aggregate(server, client, shared)


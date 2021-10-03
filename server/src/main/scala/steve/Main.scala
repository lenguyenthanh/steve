package steve

import cats.effect.IOApp
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s.host
import com.comcast.ip4s.port
import cats.effect.IO
import sttp.tapir.server.http4s.Http4sServerInterpreter

object Main extends IOApp.Simple {

  def run: IO[Unit] =
    EmberServerBuilder
      .default[IO]
      .withHost(host"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp {

        val endpoints = List(
          protocol.build.serverLogicInfallible { build =>
            IO.println(build).as(Hash(Array()))
          }
        )

        Http4sServerInterpreter[IO]()
          .toRoutes(endpoints)
          .orNotFound

      }
      .build
      .useForever

}

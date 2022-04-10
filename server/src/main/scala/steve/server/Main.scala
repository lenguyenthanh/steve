package steve.server

import cats.effect.IOApp
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s.host
import com.comcast.ip4s.port
import cats.effect.IO
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.server.ServerEndpoint

object Main extends IOApp.Simple:

  def run: IO[Unit] =
    ServerSideExecutor
      .module[IO]
      .flatMap { exe =>
        EmberServerBuilder
          .default[IO]
          .withHost(host"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp {
            Routing.instance(exe)
          }
          .build
      }
      .useForever

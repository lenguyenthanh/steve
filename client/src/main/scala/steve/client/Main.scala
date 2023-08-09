package steve.client

import cats.effect.IO
import cats.syntax.all.*
import org.http4s.ember.client.EmberClientBuilder
import sttp.tapir.client.http4s.Http4sClientInterpreter
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.effect.IOApp
import com.monovore.decline.effect.CommandIOApp
import com.monovore.decline.Opts
import cats.effect.ExitCode
import cats.effect.kernel.Resource
import java.nio.file.Path
import cats.Functor
import cats.Monad
import cats.effect.kernel.Async
import cats.Applicative
import steve.Executor
import steve.Command

object Main extends CommandIOApp("steve", "CLI for Steve", true, "0.0.1"):

  import FrontEnd.CLICommand

  def exec[F[_]: Async]: Resource[F, Executor[F]] = EmberClientBuilder
    .default[F]
    .build
    .map { client =>
      val logger = Slf4jLogger.getLogger[F]
      given Http4sClientInterpreter[F] = Http4sClientInterpreter[F]()

      ClientSideExecutor.instance[F](client)
    }

  def convertCommand[F[_]: BuildReader: Applicative]: CLICommand => F[Command] =
    case CLICommand.Build(ctx) =>
      BuildReader[F]
        .read(fs2.io.file.Path.fromNioPath(ctx) / "steve.json")
        .map(Command.Build(_))
    case CLICommand.Run(hash) => Command.Run(hash).pure[F]
    case CLICommand.List      => Command.List.pure[F]

  def eval[F[_]: Functor](exec: Executor[F]): Command => F[String] =
    case Command.Build(build) => exec.build(build).map(_.toHex)
    case Command.Run(hash) => exec.run(hash).map(state => s"SystemState \n\n ${state.prettyPrint}")
    case Command.List =>
      exec.listImages.map { images =>
        images.mkString("\n")
      }

  given BuildReader[IO] = BuildReader.instance[IO]

  val main: Opts[IO[ExitCode]] = FrontEnd
    .parseInput
    .map:
      convertCommand[IO](_)
        .flatMap { cmd =>
          steve
            .server
            .Main
            .server
            .surround:
              exec[IO].use(eval[IO](_)(cmd))
        }
        .flatMap(IO.println)
        .as(ExitCode.Success)

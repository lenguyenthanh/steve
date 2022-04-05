package steve

import cats.effect.IO
import cats.implicits.*
import org.http4s.ember.client.EmberClientBuilder
import sttp.tapir.client.http4s.Http4sClientInterpreter
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.effect.IOApp
import com.monovore.decline.effect.CommandIOApp
import com.monovore.decline.Opts
import cats.effect.ExitCode
import cats.effect.kernel.Resource
import java.nio.file.Path
import fs2.io.file.Files

object Main extends CommandIOApp("steve", "CLI for Steve", true, "0.0.1"):

  val input: Opts[IO[Command]] =
    val build: Opts[IO[Command]] = Opts
      .subcommand("build", "Build an image")(Opts.argument[Path]("path"))
      .map(p =>
        Files[IO]
          .readAll(fs2.io.file.Path.fromNioPath(p) / "steve.json")
          .through(fs2.text.utf8.decode[IO])
          .compile
          .string
          .flatMap(io.circe.parser.decode[Build](_).liftTo[IO])
          .map(Command.Build(_))
      )

    val run: Opts[IO[Command]] =
      Opts
        .subcommand("run", "run built image")(
          Opts
            .argument[String]("hash")
            .map(
              Hash
                .parse(_)
                .leftMap(Exception(_))
                .liftTo[IO]
                .map(Command.Run(_))
            )
        )

    val list: Opts[IO[Command]] =
      Opts.subcommand("list", "List known images")(Opts(Command.List.pure[IO]))

    build <+> run <+> list

  def exec: Resource[IO, Executor[IO]] = EmberClientBuilder
    .default[IO]
    .build
    .map { client =>
      val logger = Slf4jLogger.getLogger[IO]
      given Http4sClientInterpreter[IO] = Http4sClientInterpreter[IO]()

      ClientSideExecutor.instance[IO](client)
    }

  def eval(exec: Executor[IO]): Command => IO[String] =
    case Command.Build(build) => exec.build(build).map(_.toHex)
    case Command.Run(hash) => exec.run(hash).map(state => s"SystemState \n\n ${state.prettyPrint}")
    case Command.List =>
      exec.listImages.map { images =>
        images.mkString("\n")
      }

  def main: Opts[IO[ExitCode]] = input.map {
    _.flatMap { cmd =>
      exec.use(eval(_)(cmd)).flatMap(IO.println)
    }.as(ExitCode.Success)
  }

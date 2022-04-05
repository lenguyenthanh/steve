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

  enum CLICommand:
    case Build(ctx: Path)
    case Run(hash: Hash)
    case List

  val input: Opts[CLICommand] =
    val build: Opts[CLICommand] =
      Opts
        .subcommand("build", "Build an image")(Opts.argument[Path]("path").map(CLICommand.Build(_)))

    val run: Opts[CLICommand] =
      Opts
        .subcommand("run", "run built image")(
          Opts
            .argument[String]("hash")
            .mapValidated(
              Hash
                .parse(_)
                .map(CLICommand.Run(_))
                .toValidatedNel
            )
        )

    val list: Opts[CLICommand] = Opts.subcommand("list", "List known images")(Opts(CLICommand.List))

    build <+> run <+> list

  val inputIO: CLICommand => IO[Command] =
    case CLICommand.Build(ctx) =>
      Files[IO]
        .readAll(fs2.io.file.Path.fromNioPath(ctx) / "steve.json")
        .through(fs2.text.utf8.decode[IO])
        .compile
        .string
        .flatMap(io.circe.parser.decode[Build](_).liftTo[IO])
        .map(Command.Build(_))

    case CLICommand.Run(hash) => Command.Run(hash).pure[IO]
    case CLICommand.List      => Command.List.pure[IO]

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

  val main: Opts[IO[ExitCode]] = input.map {
    inputIO(_)
      .flatMap { cmd =>
        exec.use(eval(_)(cmd))
      }
      .flatMap(IO.println)
      .as(ExitCode.Success)
  }


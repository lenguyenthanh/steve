package steve.client

import cats.implicits.*
import com.monovore.decline.Opts
import java.nio.file.Path
import cats.kernel.Eq
import steve.Hash

object FrontEnd:

  enum CLICommand:
    case Build(ctx: Path)
    case Run(hash: Hash)
    case List

  object CLICommand:
    given Eq[CLICommand] = Eq.fromUniversalEquals

  val parseInput: Opts[CLICommand] =
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

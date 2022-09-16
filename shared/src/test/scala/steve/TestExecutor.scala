package steve

import cats.effect.IO
import cats.syntax.all.*

object TestExecutor:

  def instance(
    buildImpl: Map[Build, Either[Throwable, Hash]],
    runImpl: Map[Hash, Either[Throwable, SystemState]],
  ): Executor[IO] =
    new Executor[IO] {
      def build(build: Build): IO[Hash] = buildImpl(build).liftTo[IO]
      def run(hash: Hash): IO[SystemState] = runImpl(hash).liftTo[IO]
      def listImages
        : IO[List[Hash]] = runImpl.filter { case (_, r) => r.isRight }.keys.toList.pure[IO]
    }

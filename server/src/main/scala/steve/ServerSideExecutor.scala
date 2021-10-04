package steve

import cats.ApplicativeThrow
import cats.implicits.*

object ServerSideExecutor {

  def instance[F[_]: ApplicativeThrow]: Executor[F] =
    new Executor[F] {
      private val emptyHash: Hash = Hash(Array())

      def build(build: Build): F[Hash] = (build == Build.empty)
        .guard[Option]
        .as(emptyHash)
        .liftTo[F](new Throwable("Unsupported build!"))

      def run(hash: Hash): F[SystemState] = (hash == emptyHash)
        .guard[Option]
        .as(SystemState(Map.empty))
        .liftTo[F](new Throwable("Unsupported hash!"))

    }

}

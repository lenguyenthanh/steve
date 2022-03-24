package steve

import cats.implicits.*
import cats.MonadThrow
import cats.ApplicativeThrow
import cats.effect.kernel.Ref
import cats.effect.kernel.Resource
import cats.effect.std.UUIDGen

object ServerSideExecutor:

  def instance[F[_]: Interpreter: Resolver: MonadThrow]: Executor[F] =
    new Executor[F] {
      private val emptyHash: Hash = Hash(Vector.empty)
      private val emptySystem: SystemState = SystemState(Map.empty)

      def build(build: Build): F[Hash] = Resolver[F]
        .resolve(build)
        .flatMap(Interpreter[F].interpret)
        .flatMap(Resolver[F].save)

      def run(hash: Hash): F[SystemState] = (hash == emptyHash)
        .guard[Option]
        .as(emptySystem)
        .liftTo[F](Throwable("Unsupported hash!"))

    }

  def module[F[_]: MonadThrow: Ref.Make: UUIDGen]: Resource[F, Executor[F]] = {
    given Interpreter[F] = Interpreter.instance[F]
    for {
      given Resolver[F] <- Resolver.instance[F]
    } yield instance[F]
  }

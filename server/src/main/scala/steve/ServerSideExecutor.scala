package steve

import cats.implicits.*
import cats.effect.implicits.*
import cats.MonadThrow
import cats.ApplicativeThrow
import cats.effect.kernel.Ref
import cats.effect.kernel.Resource
import cats.effect.std.UUIDGen
import cats.Applicative

object ServerSideExecutor:

  def instance[F[_]: Interpreter: Resolver: Registry: MonadThrow]: Executor[F] =
    new Executor[F] {
      private val emptyHash: Hash = Hash(Vector.empty)
      private val emptySystem: SystemState = SystemState(Map.empty)

      def build(build: Build): F[Hash] = Resolver[F]
        .resolve(build)
        .flatMap(Interpreter[F].interpret)
        .flatMap(Registry[F].save)

      def run(hash: Hash): F[SystemState] = Registry[F]
        .lookup(hash)
        .flatMap(_.liftTo[F](steve.Build.Error.UnknownHash(hash)))

    }

  def module[F[_]: MonadThrow: Ref.Make: UUIDGen]: Resource[F, Executor[F]] = {
    val unit = Applicative[F].unit.toResource

    given Interpreter[F] = Interpreter.instance[F]
    for {
      given Registry[F] <- Registry.inMemory[F]
      _ <- unit
      given Resolver[F] = Resolver.instance[F]
    } yield instance[F]
  }

package steve.server

import cats.syntax.all.*
import cats.effect.implicits.*
import cats.MonadThrow
import cats.ApplicativeThrow
import cats.effect.kernel.Ref
import cats.effect.kernel.Resource
import cats.effect.std.UUIDGen
import cats.Applicative
import cats.effect.kernel.Sync
import steve.Executor
import steve.SystemState
import steve.Build
import steve.Hash

object ServerSideExecutor:

  def instance[F[_]: Interpreter: Resolver: Registry: MonadThrow]: Executor[F] =
    new Executor[F]:
      private val emptySystem: SystemState = SystemState(Map.empty)

      def build(build: Build): F[Hash] = Resolver[F]
        .resolve(build)
        .flatMap(Interpreter[F].interpret)
        .flatMap(Registry[F].save)

      def run(hash: Hash): F[SystemState] = Registry[F]
        .lookup(hash)
        .flatMap(_.liftTo[F](steve.Build.Error.UnknownHash(hash)))

      def listImages: F[List[Hash]] = Registry[F].list

  def module[F[_]: MonadThrow: Sync]: Resource[F, Executor[F]] =
    val unit = Applicative[F].unit.toResource

    given Interpreter[F] = Interpreter.instance[F]
    given Hasher[F] = Hasher.sha256Hasher[F]
    for
      given Registry[F] <- Registry.instance[F].toResource
      _ <- unit
      given Resolver[F] = Resolver.instance[F]
    yield instance[F]

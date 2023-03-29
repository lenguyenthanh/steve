package steve.server

import cats.effect.implicits.*
import cats.syntax.all.*
import cats.MonadThrow
import cats.effect.kernel.Ref
import cats.effect.kernel.Resource
import steve.Build.Error.*
import cats.effect.std.UUIDGen
import steve.Build
import steve.SystemState

trait Resolver[F[_]]:
  def resolve(build: Build): F[ResolvedBuild]

object Resolver:

  def apply[F[_]](
    using ev: Resolver[F]
  ) = ev

  def instance[F[_]: Registry: MonadThrow]: Resolver[F] =
    new Resolver:

      private val resolveCommand: Build.Command => ResolvedBuild.Command =
        case Build.Command.Upsert(k, v) => ResolvedBuild.Command.Upsert(k, v)
        case Build.Command.Delete(k)    => ResolvedBuild.Command.Delete(k)

      private def resolveBase(base: Build.Base): F[SystemState] =
        base match
          case Build.Base.EmptyImage => SystemState.empty.pure[F]
          case Build.Base.ImageReference(hash) =>
            Registry[F]
              .lookup(hash)
              .flatMap(_.liftTo[F](UnknownBase(hash)))

      def resolve(build: Build): F[ResolvedBuild] = resolveBase(build.base)
        .map { sys =>
          ResolvedBuild(sys, build.commands.map(resolveCommand))
        }

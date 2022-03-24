package steve

import cats.effect.implicits.*
import cats.implicits.*
import cats.MonadThrow
import cats.effect.kernel.Ref
import cats.effect.kernel.Resource
import steve.Build.Error.*
import cats.effect.std.UUIDGen

trait Resolver[F[_]]:
  def resolve(build: Build): F[ResolvedBuild]
  def save(system: SystemState): F[Hash]
  def lookup(hash: Hash): F[SystemState]

object Resolver:

  def apply[F[_]](using ev: Resolver[F]) = ev

  val emptyHash: Hash = Hash(Vector.empty)
  val emptySystem: SystemState = SystemState(Map.empty)

  def instance[
    F[_]: MonadThrow: Ref.Make: UUIDGen
  ]: Resource[F, Resolver[F]] = Ref[F].of(Map(emptyHash -> emptySystem)).toResource.map { ref =>
    new Resolver {

      private val resolveCommand: Build.Command => ResolvedBuild.Command = {
        case Build.Command.Upsert(k, v) => ResolvedBuild.Command.Upsert(k, v)
        case Build.Command.Delete(k)    => ResolvedBuild.Command.Delete(k)
      }

      def lookupInternal(hash: Hash): F[Option[SystemState]] = ref
        .get
        .map(_.get(hash))

      private def resolveBase(base: Build.Base): F[SystemState] =
        base match {
          case Build.Base.EmptyImage =>
            lookupInternal(emptyHash)
              .flatMap(_.liftTo[F](Throwable("Impossible! Hash not found for emptyImage")))
          case Build.Base.ImageReference(hash) =>
            lookupInternal(hash)
              .flatMap(_.liftTo[F](UnknownBase(hash)))
        }

      def resolve(build: Build): F[ResolvedBuild] = resolveBase(build.base)
        .map { sys =>
          ResolvedBuild(sys, build.commands.map(resolveCommand))
        }

      def save(system: SystemState): F[Hash] = UUIDGen[F].randomUUID.flatMap { uuid =>
        ref.modify { map =>
          val hash = map
            .collectFirst { case (k, `system`) => k }
            .getOrElse(Hash(uuid.toString.getBytes.toVector))
          (map + (hash -> system), hash)
        }
      }

      def lookup(hash: Hash): F[SystemState] = lookupInternal(hash)
        .flatMap(_.liftTo[F](UnknownHash(hash)))

    }
  }

end Resolver

package steve

import cats.effect.implicits.*
import cats.implicits.*
import cats.MonadThrow
import cats.effect.kernel.Ref
import cats.effect.kernel.Resource
import steve.Build.Error.*
import cats.effect.std.UUIDGen

trait Registry[F[_]]:
  def save(system: SystemState): F[Hash]
  def lookup(hash: Hash): F[Option[SystemState]]

object Registry:

  def apply[F[_]](using ev: Registry[F]) = ev

  val emptyHash: Hash = Hash(Vector.empty)
  val emptySystem: SystemState = SystemState(Map.empty)

  def inMemory[
    F[_]: MonadThrow: Ref.Make: UUIDGen
  ]: Resource[F, Registry[F]] = Ref[F].of(Map(emptyHash -> emptySystem)).toResource.map { ref =>
    new Registry {

      def save(system: SystemState): F[Hash] = UUIDGen[F].randomUUID.flatMap { uuid =>
        ref.modify { map =>
          val hash = map
            .collectFirst { case (k, `system`) => k }
            .getOrElse(Hash(uuid.toString.getBytes.toVector))
          (map + (hash -> system), hash)
        }
      }

      def lookup(hash: Hash): F[Option[SystemState]] = ref
        .get
        .map(_.get(hash))

    }
  }

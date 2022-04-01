package steve

import cats.effect.implicits.*
import cats.implicits.*
import cats.MonadThrow
import cats.effect.kernel.Ref
import steve.Build.Error.*

trait Registry[F[_]]:
  def save(system: SystemState): F[Hash]
  def lookup(hash: Hash): F[Option[SystemState]]

object Registry:

  def apply[F[_]](using ev: Registry[F]) = ev

  def inMemory[
    F[_]: MonadThrow: Ref.Make: Hasher
  ]: F[Registry[F]] = Ref[F].of(Map.empty[Hash, SystemState]).map { ref =>
    new Registry {

      def save(system: SystemState): F[Hash] = Hasher[F].hash(system).flatMap { hash =>
        ref.modify { map =>
          (map + (hash -> system), hash)
        }
      }

      def lookup(hash: Hash): F[Option[SystemState]] = ref
        .get
        .map(_.get(hash))

    }
  }

  def instance[F[_]: MonadThrow: Ref.Make: Hasher] = inMemory

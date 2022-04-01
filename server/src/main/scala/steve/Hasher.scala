package steve

import cats.effect.implicits.*
import cats.implicits.*
import steve.Build.Error.*
import cats.effect.kernel.Sync
import java.security.MessageDigest
import cats.Functor

trait Hasher[F[_]]:
  def hash(system: SystemState): F[Hash]

object Hasher:

  def apply[F[_]](using ev: Hasher[F]) = ev

  // Sync because it is not threadsafe
  def sha256Hasher[F[_]: Sync]: Hasher[F] =
    system =>
      val systemBytes =
        system.all.toList.sortBy(_._1).map { case (k, v) => s"$k:$v" }.mkString.getBytes
      Sync[F]
        .delay {
          MessageDigest
            .getInstance("SHA-256")
            .digest(systemBytes)
        }
        .map(bytes => Hash(bytes.toVector))

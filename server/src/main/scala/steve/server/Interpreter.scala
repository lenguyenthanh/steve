package steve.server

import cats.implicits.*
import cats.Applicative
import cats.data.State
import monocle.syntax.all.*
import steve.SystemState

trait Interpreter[F[_]]:
  def interpret(build: ResolvedBuild): F[SystemState]

object Interpreter:
  def apply[F[_]](using F: Interpreter[F]): Interpreter[F] = F

  def instance[F[_]: Applicative]: Interpreter[F] =
    new Interpreter[F] {

      // can replace monocle with upsert and delete functions
      private val transisition: ResolvedBuild.Command => State[SystemState, Unit] =
        case ResolvedBuild.Command.Upsert(k, v) => State.modify(_.focus(_.all).modify(_ + (k -> v)))
        case ResolvedBuild.Command.Delete(k)    => State.modify(_.focus(_.all).modify(_ - k))

      def interpret(build: ResolvedBuild): F[SystemState] = build
        .commands
        .traverse(transisition)
        .runS(build.base)
        .value
        .pure[F]

    }

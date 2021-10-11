package steve

import munit.CatsEffectSuite
import cats.effect.IO
import sttp.tapir.client.http4s.Http4sClientInterpreter
import org.http4s.client.Client
import cats.implicits._

class CompatTests extends CatsEffectSuite {

  def testExecutor(
    buildImpl: Map[Build, Either[Throwable, Hash]],
    runImpl: Map[Hash, Either[Throwable, SystemState]],
  ): Executor[IO] =
    new Executor[IO] {
      def build(build: Build) = buildImpl(build).liftTo[IO]
      def run(hash: Hash) = runImpl(hash).liftTo[IO]
    }

  given Http4sClientInterpreter[IO] = Http4sClientInterpreter[IO]()

  val goodBuild: Build = Build.empty
  val goodBuildResult: Hash = Hash(Vector.empty)
  val goodHash: Hash = Hash(Vector.empty)
  val goodRunResult: SystemState = SystemState(Map.empty)

  val exec: Executor[IO] = testExecutor(
    Map(
      goodBuild -> goodBuildResult.asRight
    ),
    Map(
      goodHash -> goodRunResult.asRight
    ),
  )

  val client = ClientSideExecutor.instance[IO](
    Client.fromHttpApp(Routing.instance(exec))
  )

  test("Build image - success") {

    assertIO(
      client.build(goodBuild),
      goodBuildResult,
    )
  }

  test("Run hash - success") {

    assertIO(
      client.run(goodHash),
      goodRunResult,
    )
  }

}

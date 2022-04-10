package steve.server

import munit.CatsEffectSuite
import cats.effect.IO
import steve.Build

class ExecutorTests extends CatsEffectSuite:
  val execR = ServerSideExecutor.module[IO]

  test("Build empty image") {

    assertIO(
      execR.use(exec => exec.build(Build.empty).flatMap(exec.run)).map(_.all),
      Map.empty,
    )
  }

package steve

import munit.CatsEffectSuite
import cats.Id

class ExecutorTests extends CatsEffectSuite {
  val exec = ServerSideExecutor.instance[Either[Throwable, *]]

  test("Build empty image") {

    assertEquals(
      exec.build(Build.empty).flatMap(exec.run).map(_.getAll),
      Right(Map.empty),
    )
  }
}

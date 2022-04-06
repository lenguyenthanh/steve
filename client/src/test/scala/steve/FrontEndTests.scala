package steve

import weaver.*
import com.monovore.decline
import cats.kernel.Eq
import com.monovore.decline.Help
import java.nio.file.Paths

object FrontEndTests extends FunSuite:

  import FrontEnd.*
  given Eq[Help] = Eq.fromUniversalEquals

  def testCommand(
    args: String*
  ) = decline.Command("test", "Test Command")(FrontEnd.parseInput).parse(args)

  test("list command") {
    assert.eql(testCommand("list"), Right(CLICommand.List))
  }

  test("build command") {
    assert.eql(testCommand("build", "."), Right(CLICommand.Build(Paths.get("."))))
  }

  test("run command") {
    val hash = "4162fddd39a3e4225e8e2392eced237fbeb34e6e218b5647d27bd4d2b9c0da24"
    assert.eql(
      testCommand("run", hash),
      Right(CLICommand.Run(Hash.parse(hash).toOption.get)),
    )
  }

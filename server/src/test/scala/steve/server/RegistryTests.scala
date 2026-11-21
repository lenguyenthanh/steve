package steve.server

import weaver.*
import weaver.scalacheck.Checkers
import cats.effect.IO
import org.scalacheck.Prop.forAll
import Arbitraries.given
import cats.syntax.all.*
import steve.SystemState
import steve.Hash

object RegistryTests extends SimpleIOSuite with Checkers:

  given Hasher[IO] = Hasher.sha256Hasher[IO]
  val registryR = Registry.instance[IO]

  test("save -> lookup returns the same system"):
    forall { (system: SystemState) =>
      registryR.flatMap { registry =>
        for
          hash <- registry.save(system)
          result <- registry.lookup(hash)
        yield expect(result.contains(system))
      }
    }

  test("save is idempotent"):
    forall { (system: SystemState, systems: List[SystemState], hash: Hash) =>
      registryR.flatMap { registry =>
        for
          hash1 <- registry.save(system)
          _ <- systems.traverse_(registry.save)
          hash2 <- registry.save(system)
        yield expect(hash1 == hash2)
      }
    }

  test("lookup is idempotent"):
    forall { (systems: List[SystemState], otherSystems: List[SystemState], hash: Hash) =>
      registryR.flatMap { registry =>
        for
          _ <- systems.traverse_(registry.save)
          result1 <- registry.lookup(hash)
          _ <- otherSystems.traverse_(registry.save)
          result2 <- registry.lookup(hash)
        yield expect(result1 == result2)
      }
    }

  test("list on an empty registry is empty"):
    registryR.flatMap { registry =>
      for result <- registry.list
      yield expect(result.isEmpty)
    }

  test("save + list returns saved systems"):
    forall { (systems: List[SystemState]) =>
      registryR.flatMap { registry =>
        for
          hashes <- systems.traverse(registry.save)
          list <- registry.list
        yield expect(list.toSet == hashes.toSet)
      }
    }

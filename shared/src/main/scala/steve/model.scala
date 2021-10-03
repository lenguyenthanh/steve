package steve

import steve.Build.Command

enum Command {
  case Build(build: steve.Build)
  case Run(hash: Hash)
}

case class Build(
  base: Build.Base,
  commands: List[Build.Command],
)

object Build {
  enum Base {
    case EmptyImage
    case ImageReference(hash: Hash)
  }

  enum Command {
    case Upsert(key: String, value: String)
    case DELETE(key: String)
  }

  val empty = Build(Build.Base.EmptyImage, Nil)
}

case class Hash(value: Array[Byte])

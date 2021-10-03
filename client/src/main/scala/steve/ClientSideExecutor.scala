package steve

object ClientSideExecutor {

  def instance[F[_]]: Executor[F] = new Executor[F] {
    private val emptyHash: Hash = Hash(Array())

    def build(build: Build): F[Hash] = ???

    def run(hash: Hash): F[SystemState] = ???
  }

}

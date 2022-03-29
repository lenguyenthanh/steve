package steve

object protocol:
  import sttp.tapir.*
  import sttp.tapir.json.circe.*
  import sttp.tapir.generic.auto.*

  private val base = infallibleEndpoint.in("api")

  val build = base
    .put
    .in("build")
    .in(jsonBody[Build])
    .out(jsonBody[Hash])
    .errorOut(jsonBody[Build.Error])

  val run = base
    .post
    .in("run")
    .in(jsonBody[Hash])
    .out(jsonBody[SystemState])

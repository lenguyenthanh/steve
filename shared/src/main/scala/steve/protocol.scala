package steve

import sttp.tapir.Endpoint
import sttp.capabilities.fs2.Fs2Streams
import sttp.model.StatusCode

object protocol:
  import sttp.tapir.*
  import sttp.tapir.json.circe.*
  import sttp.tapir.generic.auto.*

  private val base = infallibleEndpoint.in("api")

  val build: PublicEndpoint[Build, Build.Error, Hash, Any] = base
    .put
    .in("build")
    .in(jsonBody[Build])
    .out(jsonBody[Hash])
    .errorOut(jsonBody[Build.Error])

  def buildStream[F[_]] =
    base
      .put
      .in("build")
      .in(jsonBody[Build])
      .out(streamTextBody(Fs2Streams[F])(CodecFormat.Json.apply(), None))
      .errorOut(statusCode(StatusCode.UnprocessableEntity).and(jsonBody[Build.Error]))

  val run: PublicEndpoint[Hash, Nothing, SystemState, Any] = base
    .post
    .in("run")
    .in(jsonBody[Hash])
    .out(jsonBody[SystemState])

  val listImages: PublicEndpoint[Unit, Nothing, List[Hash], Any] = base
    .get
    .in("images")
    .out(jsonBody[List[Hash]])


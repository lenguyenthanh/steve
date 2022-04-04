package steve

import org.http4s.HttpApp
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import cats.effect.kernel.Async
import sttp.tapir.server.http4s.Http4sServerOptions
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.model.StatusCode
import sttp.tapir.server.model.ValuedEndpointOutput

object Routing:

  def instance[F[_]: Async](exec: Executor[F]): HttpApp[F] =
    val endpoints: List[ServerEndpoint[Any, F]] = List(
      protocol.build.serverLogicRecoverErrors(exec.build),
      protocol.run.serverLogicSuccess(exec.run),
      protocol.listImages.serverLogicSuccess(_ => exec.listImages),
    )

    Http4sServerInterpreter[F](
      Http4sServerOptions
        .customInterceptors[F, F]
        .exceptionHandler(ex =>
          Some(
            ValuedEndpointOutput(
              jsonBody[GenericServerError].and(statusCode(StatusCode.InternalServerError)),
              GenericServerError("server failed"),
            )
          )
        )
        .options
    )
      .toRoutes(endpoints)
      .orNotFound

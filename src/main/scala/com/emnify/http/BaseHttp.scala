package com.emnify.http

import cats.effect.kernel.Async
import cats.implicits._
import com.emnify.common.{Fail, JsonSupport}
import com.emnify.http.BaseHttp.ErrorResponse
import com.softwaremill.tagging.{@@, Tagger}
import log.effect.LogWriter
import sttp.model.StatusCode
import sttp.tapir.Codec.PlainCodec
import sttp.tapir.generic.auto.SchemaDerivation
import sttp.tapir.{EndpointOutput, PublicEndpoint, Schema}

class BaseHttp[F[_]: Async](implicit logger: LogWriter[F]) extends JsonSupport with TapirSchemas {

  val jsonErrorOutOutput: EndpointOutput[ErrorResponse] = jsonBody[ErrorResponse]

  val failOutput: EndpointOutput[(StatusCode, ErrorResponse)] = statusCode.and(jsonErrorOutOutput)

  val baseEndpoint: PublicEndpoint[Unit, (StatusCode, ErrorResponse), Unit, Any] =
    endpoint.errorOut(failOutput)

  private val InternalServerError = (StatusCode.InternalServerError, "Internal server error")

  private val failToResponseData: Fail => (StatusCode, String) = {
    case Fail.NotFound(what)      => (StatusCode.NotFound, what)
    case Fail.Conflict(msg)       => (StatusCode.Conflict, msg)
    case Fail.IncorrectInput(msg) => (StatusCode.BadRequest, msg)
    case _                        => InternalServerError
  }

  implicit class FOut[T](f: F[T]) {

    def toOut: F[Either[(StatusCode, ErrorResponse), T]] = {
      f.map(t => t.asRight[(StatusCode, ErrorResponse)]).recoverWith { case f: Fail =>
        val (statusCode, message) = failToResponseData(f)
        logger.warn(s"Request fail: $message")
        (statusCode, ErrorResponse(message)).asLeft[T].pure[F]
      }
    }
  }
}

object BaseHttp {
  case class ErrorResponse(message: String)
}

trait TapirSchemas extends SchemaDerivation {

  implicit def schemaForTagged[U, T](implicit uc: Schema[U]): Schema[U @@ T] = uc.asInstanceOf[Schema[U @@ T]]

  implicit def taggedPlainCodec[U, T](implicit uc: PlainCodec[U]): PlainCodec[U @@ T] =
    uc.map(_.taggedWith[T])(identity)
}

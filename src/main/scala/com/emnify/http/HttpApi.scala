package com.emnify.http

import cats.effect.Async
import cats.implicits._
import com.emnify.domain.{AggregationService, ProductCode, ProductOfferAggregation}
import log.effect.LogWriter
import org.http4s.HttpRoutes
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.SwaggerUIOptions
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.{EndpointInput, Tapir}

class HttpApi[F[_]: Async](service: AggregationService[F])(implicit log: LogWriter[F]) extends BaseHttp[F] with Tapir {
  import HttpApi._

  private val interpreter = Http4sServerInterpreter[F]()

  private val apiContextPath = List("api", "v1")

  private val getAggregationEndpoint =
    baseEndpoint
      .in("aggregations")
      .post
      .in(jsonBody[AggregationRequest])
      .out(jsonBody[AggregationResponse])
      .serverLogic { request =>
        service
          .getAggregation(ProductCode(request.productCode))
          .map(AggregationResponse(_))
          .toOut
      }

  private val closeAggregationEndpoint =
    baseEndpoint.post
      .in("aggregations" / "close")
      .in(jsonBody[CloseAggregationRequest])
      .out(jsonBody[CloseAggregationResponse])
      .serverLogic { request =>
        service
          .closeAggregation(ProductCode(request.productCode), request.closeOfferAggregationThreshold)
          .map(CloseAggregationResponse(_))
          .toOut
      }

  lazy val mainEndpoints = List(getAggregationEndpoint, closeAggregationEndpoint).map(se =>
    se.prependSecurityIn(apiContextPath.foldLeft(emptyInput: EndpointInput[Unit])(_ / _))
  )
  lazy val docsEndpoints = SwaggerInterpreter(swaggerUIOptions = SwaggerUIOptions.default.copy(contextPath = apiContextPath))
    .fromServerEndpoints(mainEndpoints, "Emnify", "1.0")

  lazy val allEndpoints: List[ServerEndpoint[Any, F]] = mainEndpoints ++ docsEndpoints

  def routes: HttpRoutes[F] = interpreter.toRoutes(allEndpoints)
}

object HttpApi {

  case class AggregationRequest(productCode: String)
  case class AggregationResponse(productOfferAggregation: ProductOfferAggregation)

  case class CloseAggregationRequest(productCode: String, closeOfferAggregationThreshold: Option[Long])
  case class CloseAggregationResponse(productCode: String)
}

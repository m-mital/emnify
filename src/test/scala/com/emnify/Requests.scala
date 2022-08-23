package com.emnify

import cats.effect.IO
import com.emnify.common.TestSupport
import com.emnify.domain.ProductCode
import com.emnify.http.HttpApi.AggregationRequest
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import sttp.client3.{Response, SttpBackend, UriContext, basicRequest}

class Requests(backend: SttpBackend[IO, Any]) extends TestSupport {

  private val basePath = "http://localhost:8080/api/v1"

  def getAggregation(productCode: ProductCode): Response[Either[String, String]] = {
    basicRequest
      .post(uri"$basePath/aggregations")
      .body(AggregationRequest(productCode).asJson.noSpaces)
      .send(backend)
      .unwrap
  }

  // TODO: I'll finish tests before technical review. Check README for more details :)
}

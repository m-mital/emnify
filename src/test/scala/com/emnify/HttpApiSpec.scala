package com.emnify

import cats.effect.IO
import com.emnify.common.TestSupport
import com.emnify.domain.ProductCode
import io.circe.generic.AutoDerivation
import org.scalatest.Suite
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import sttp.client3.testing.SttpBackendStub
import sttp.tapir.server.stub.TapirStubInterpreter

class HttpApiSpec extends AnyFlatSpec with Matchers with TestSupport with AutoDerivation {
  self: Suite =>

  private val stub: SttpBackendStub[IO, Fs2Streams[IO]] = AsyncHttpClientFs2Backend.stub[IO]

  private lazy val serverStub: SttpBackend[IO, Any] =
    TapirStubInterpreter[IO, Any](stub)
      .whenServerEndpointsRunLogic(???)
      .backend()

  lazy val requests = new Requests(serverStub)

  "/aggregations" should "get an aggregation" in {

    // given
    val product = ProductCode("wash-machine-lg")

    // TODO: I'll finish tests before technical review. Check README for more details :)
    // when
    // val response = requests.getAggregation(product)

    // then
    // response.code shouldBe StatusCode.Ok
    // response.body.shouldDeserializeTo[AggregationResponse]
  }
}

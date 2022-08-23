package com.emnify

import cats.effect.IO
import com.emnify.common.{BasicDbTestSupport, Fail}
import com.emnify.domain._
import com.emnify.infrastructure.{AggregationRepository, AggregationService}
import doobie.ConnectionIO
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AggregationServiceSpec extends AnyFlatSpec with Matchers with BasicDbTestSupport {

  private implicit val runtime = cats.effect.unsafe.IORuntime.global

  private val repository = AggregationRepository()
  private val service = AggregationService[IO, ConnectionIO](repository, 3, gToF)

  val washMachine = "wash-machine-bosh"
  val fridge = "fridge-bosh"

  val defaultClosingAggregationThreshold = 3

  behavior of "Aggregation Service"

  it should "return an aggregation with a given code" in {

    val aggregations = List(
      ProductOfferAggregation(ProductCode(washMachine), MinPrice(1199), MaxPrice(1199), AvgPrice(1199), 1, AggregationStatus.Open),
      ProductOfferAggregation(ProductCode(fridge), MinPrice(2199), MaxPrice(2199), AvgPrice(2199), 1, AggregationStatus.Open)
    )

    service.insertOrUpdateAggregations(aggregations).unsafeRunSync()
    val aggregation = service.getAggregation(ProductCode(washMachine))

    aggregation.unsafeRunSync() shouldBe aggregations.head
  }

  it should "return an error when closing non-existent Aggregation" in {

    val aggregations = List(
      ProductOfferAggregation(ProductCode(washMachine), MinPrice(1199), MaxPrice(1199), AvgPrice(1199), 1, AggregationStatus.Open),
      ProductOfferAggregation(ProductCode(fridge), MinPrice(2199), MaxPrice(2199), AvgPrice(2199), 1, AggregationStatus.Open)
    )

    service.insertOrUpdateAggregations(aggregations).unsafeRunSync()
    val closeAggregation = service.closeAggregation(ProductCode("non-existent-product"), Some(3))

    the[Fail] thrownBy closeAggregation.unsafeRunSync() shouldBe
      Fail.NotFound(s"Aggregation with the code [non-existent-product] not found")
  }

  it should "return an error due to insufficient number of offers in the aggregation" in {

    val aggregations = List(
      ProductOfferAggregation(ProductCode(washMachine), MinPrice(1199), MaxPrice(1199), AvgPrice(1199), 1, AggregationStatus.Open),
      ProductOfferAggregation(ProductCode(fridge), MinPrice(2199), MaxPrice(2199), AvgPrice(2199), 1, AggregationStatus.Open)
    )

    service.insertOrUpdateAggregations(aggregations).unsafeRunSync()
    val closeAggregation = service.closeAggregation(ProductCode(washMachine), None)

    the[Fail] thrownBy closeAggregation.unsafeRunSync() shouldBe Fail.IncorrectInput(
      s"Error during closing aggregation with the code [$washMachine]. Number of offers must be greater than or equal to $defaultClosingAggregationThreshold"
    )
  }

  it should "should override default closing aggregation threshold and close it" in {

    val customClosingAggregationThreshold = 1
    val anotherCustomClosingAggregationThreshold = 5

    val aggregations = List(
      ProductOfferAggregation(washMachine, MinPrice(1199), MaxPrice(1199), AvgPrice(1199), 1, AggregationStatus.Open),
      ProductOfferAggregation(fridge, MinPrice(2199), MaxPrice(2199), AvgPrice(2199), 4, AggregationStatus.Open)
    )

    service.insertOrUpdateAggregations(aggregations).unsafeRunSync()
    val closeWashMachineAggregation = service.closeAggregation(ProductCode(washMachine), Some(customClosingAggregationThreshold))
    val closeFridgeAggregation = service.closeAggregation(ProductCode(fridge), Some(anotherCustomClosingAggregationThreshold))

    closeWashMachineAggregation.unsafeRunSync() shouldBe washMachine
    the[Fail] thrownBy closeFridgeAggregation.unsafeRunSync() shouldBe Fail.IncorrectInput(
      s"Error during closing aggregation with the code [$fridge]. Number of offers must be greater than or equal to $anotherCustomClosingAggregationThreshold"
    )
  }

  it should "should update existing aggregations" in {

    val aggregations = List(
      ProductOfferAggregation(ProductCode("wash-machine-bosch"), MinPrice(1199), MaxPrice(1199), AvgPrice(1199), 1, AggregationStatus.Open),
      ProductOfferAggregation(ProductCode("fridge-bosch"), MinPrice(2199), MaxPrice(2199), AvgPrice(2199), 1, AggregationStatus.Open)
    )

    val aggregationsToUpdate =
      List(
        ProductOfferAggregation(
          ProductCode("wash-machine-bosch"),
          MinPrice(1099),
          MaxPrice(1399),
          AvgPrice(1249),
          5,
          AggregationStatus.Open
        )
      )

    service.insertOrUpdateAggregations(aggregations).unsafeRunSync()
    service.insertOrUpdateAggregations(aggregationsToUpdate).unsafeRunSync()
    val washMachineAggregation = service.getAggregation(ProductCode("wash-machine-bosch")).unsafeRunSync()

    washMachineAggregation shouldBe
      ProductOfferAggregation("wash-machine-bosch", MinPrice(2298), MaxPrice(2598), AvgPrice(2448), 6, AggregationStatus.Open)
  }
}

package com.emnify

import com.emnify.common.BasicDbTestSupport
import com.emnify.domain._
import com.emnify.infrastructure.AggregationRepository
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AggregationRepositorySpec extends AnyFlatSpec with Matchers with BasicDbTestSupport {

  private val repository = AggregationRepository()

  behavior of "aggregation repository"

  it should "save aggregations to DB" in {

    val aggregations = List(
      ProductOfferAggregation(ProductCode("wash-machine-bosch"), MinPrice(1199), MaxPrice(1199), AvgPrice(1199), 2, AggregationStatus.Open),
      ProductOfferAggregation(ProductCode("fridge-LG"), MinPrice(1199), MaxPrice(1199), AvgPrice(1199), 5, AggregationStatus.Open)
    )

    repository.insertOrUpdateAggregations(aggregations).unwrap(xa)

    val savedWashMachine = repository.getAggregation(ProductCode("wash-machine-bosch")).unwrap(xa)
    val savedFridge = repository.getAggregation(ProductCode("fridge-LG")).unwrap(xa)

    savedWashMachine shouldBe Some(aggregations.head)

    savedFridge shouldBe Some(aggregations.tail.head)
  }

  it should "should update existing aggregations" in {

    val aggregations = List(
      ProductOfferAggregation(ProductCode("wash-machine-bosch"), MinPrice(1199), MaxPrice(1199), AvgPrice(1199), 1, AggregationStatus.Open),
      ProductOfferAggregation(ProductCode("fridge-bosch"), MinPrice(2199), MaxPrice(2199), AvgPrice(2199), 1, AggregationStatus.Open)
    )

    val aggregationsToUpdate = List(
      ProductOfferAggregation(ProductCode("wash-machine-bosch"), MinPrice(1099), MaxPrice(1399), AvgPrice(1249), 5, AggregationStatus.Open),
      ProductOfferAggregation(ProductCode("fridge-LG"), MinPrice(999), MaxPrice(1299), AvgPrice(1149), 10, AggregationStatus.Open)
    )

    repository.insertOrUpdateAggregations(aggregations).unwrap(xa)
    repository.insertOrUpdateAggregations(aggregationsToUpdate).unwrap(xa)

    val washMachineAggregation = repository.getAggregation(ProductCode("wash-machine-bosch")).unwrap(xa)
    val fridgeLGAggregation = repository.getAggregation(ProductCode("fridge-LG")).unwrap(xa)

    washMachineAggregation shouldBe Some(
      ProductOfferAggregation("wash-machine-bosch", MinPrice(2298), MaxPrice(2598), AvgPrice(2448), 6, AggregationStatus.Open)
    )

    fridgeLGAggregation shouldBe Some(
      ProductOfferAggregation("fridge-LG", MinPrice(999), MaxPrice(1299), AvgPrice(1149), 10, AggregationStatus.Open)
    )
  }

  it should "return None if there's no aggregation with a given code in the db" in {

    val aggregations = List(
      ProductOfferAggregation(ProductCode("wash-machine-bosh"), MinPrice(1199), MaxPrice(1199), AvgPrice(1199), 1, AggregationStatus.Open),
      ProductOfferAggregation(ProductCode("fridge-bosh"), MinPrice(2199), MaxPrice(2199), AvgPrice(2199), 1, AggregationStatus.Open)
    )

    repository.insertOrUpdateAggregations(aggregations).unwrap(xa)

    val aggregation = repository.getAggregation(ProductCode("bosh")).unwrap(xa)

    aggregation should be(None)
  }

  it should "return None if there's no aggregation in DB" in {

    val product = ProductCode("fridge")

    val aggregation = repository.getAggregation(product).unwrap(xa)

    aggregation should be(None)
  }

  it should " not allow to close an aggregation because there are less offers than a threshold number" in {

    val aggregations = List(
      ProductOfferAggregation(ProductCode("wash-machine-bosch"), MinPrice(1199), MaxPrice(1199), AvgPrice(1199), 1, AggregationStatus.Open),
      ProductOfferAggregation(ProductCode("fridge-bosch"), MinPrice(2199), MaxPrice(2199), MaxPrice(2199), 1, AggregationStatus.Open)
    )

    repository.insertOrUpdateAggregations(aggregations).unwrap(xa)

    val aggregation = repository.getAggregation(ProductCode("wash-machine-bosch")).unwrap(xa)

    aggregation should be(Some(aggregations.head))
  }

  it should "allow to close an aggregation if there are more offers than a threshold number" in {

    val aggregations = List(
      ProductOfferAggregation(ProductCode("wash-machine-bosch"), Price(1199), Price(1199), Price(1199), 2, AggregationStatus.Open),
      ProductOfferAggregation(ProductCode("fridge-LG"), Price(1199), Price(1199), Price(1199), 5, AggregationStatus.Open)
    )

    repository.insertOrUpdateAggregations(aggregations).unwrap(xa)

    val aggregation = repository.getAggregation(ProductCode("wash-machine-bosch")).unwrap(xa)

    aggregation should be(Some(aggregations.head))
    aggregation should be(Some(aggregations.head))

    aggregation.map(_.noOfOffers should be(2))

    repository.closeAggregation(ProductCode("wash-machine-bosch")).unwrap(xa)

    val updatedAggr = repository.getAggregation(ProductCode("wash-machine-bosch")).unwrap(xa)

    updatedAggr.map(_.status shouldBe AggregationStatus.Closed)

  }

  it should "not allow to aggregate offers if aggregation is closed" in {

    val aggregations = List(
      ProductOfferAggregation(ProductCode("wash-machine-bosch"), MinPrice(1199), MaxPrice(1199), AvgPrice(1199), 2, AggregationStatus.Open),
      ProductOfferAggregation(ProductCode("fridge-LG"), MinPrice(1199), MaxPrice(1199), AvgPrice(1199), 5, AggregationStatus.Open)
    )

    repository.insertOrUpdateAggregations(aggregations).unwrap(xa)
    repository.closeAggregation(ProductCode("wash-machine-bosch")).unwrap(xa)

    val aggregation = repository.getAggregation(ProductCode("wash-machine-bosch")).unwrap(xa)

    aggregation shouldBe Some(
      ProductOfferAggregation(
        ProductCode("wash-machine-bosch"),
        MinPrice(1199),
        MaxPrice(1199),
        AvgPrice(1199),
        2,
        AggregationStatus.Closed
      )
    )
    aggregation.map(_.noOfOffers should be(2))

    repository.insertOrUpdateAggregations(aggregations).unwrap(xa)

    val updatedAggr = repository.getAggregation(ProductCode("wash-machine-bosch")).unwrap(xa)
    updatedAggr.map(_.status shouldBe AggregationStatus.Closed)
    updatedAggr.map(_.noOfOffers shouldBe 2)
  }
}

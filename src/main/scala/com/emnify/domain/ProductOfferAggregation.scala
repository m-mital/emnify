package com.emnify.domain

import sttp.tapir.Schema

case class ProductOfferAggregation(
    productCode: String,
    minPrice: MinPrice,
    maxPrice: MaxPrice,
    avgPrice: AvgPrice,
    noOfOffers: Long,
    status: AggregationStatus
)

object ProductOfferAggregation {
  implicit val schemaForProductAggregation: Schema[ProductOfferAggregation] = Schema.string
}

package com.emnify.domain

trait AggregationRepository[G[_]] {
  def closeAggregation(code: ProductCode): G[ProductCode]
  def getAggregation(code: ProductCode): G[Option[ProductOfferAggregation]]
  def updateAggregations(aggregations: List[ProductOfferAggregation]): G[Int]
  def insertAggregations(aggregations: List[ProductOfferAggregation]): G[Int]
  def insertOrUpdateAggregations(aggregations: List[ProductOfferAggregation]): G[Int]
}

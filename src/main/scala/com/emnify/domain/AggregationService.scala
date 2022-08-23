package com.emnify.domain

import cats.~>

trait AggregationService[F[_]] {

  type GG[_]

  def closeAggregation(code: ProductCode, closeOfferAggregationThreshold: Option[Long]): F[ProductCode]

  def getAggregation(code: ProductCode): F[ProductOfferAggregation]

  def insertOrUpdateAggregations(aggregations: List[ProductOfferAggregation]): F[Int]

  def gToF: GG ~> F
}

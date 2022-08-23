package com.emnify.infrastructure

import cats.Applicative
import com.emnify.data.DataSource
import com.emnify.domain.{AggregationService, AggregationStatus, AvgPrice, Offer, Price, ProductOfferAggregation}
import fs2._
import log.effect.LogWriter

import scala.math.BigDecimal.RoundingMode

class ProductOfferAggregationProgram[F[_]: Applicative](
    dataSource: DataSource[F],
    service: AggregationService[F]
)(implicit log: LogWriter[F]) {

  private def calculateAvgPrice(prices: List[Price]) = {
    val avgPrice = prices.foldLeft(BigDecimal(0))(_ + _) / prices.size
    avgPrice.setScale(2, RoundingMode.HALF_UP)
  }

  private val aggregateOffers: Pipe[F, Chunk[Offer], List[ProductOfferAggregation]] =
    _.map(
      _.toList
        .groupMap(_.productCode)(_.price)
        .map { case (productCode, prices) =>
          val avgPrice = calculateAvgPrice(prices)
          ProductOfferAggregation(productCode, prices.min, prices.max, AvgPrice(avgPrice), prices.size, AggregationStatus.Open)
        }
        .toList
    )

  val program: Stream[F, List[ProductOfferAggregation]] = dataSource.source
    .chunkN(20)
    .through(aggregateOffers)
    .evalTap(offers => log.debug(s"Aggregated products with codes: (${offers.map(_.productCode).mkString(", ")})"))
    .evalTap(service.insertOrUpdateAggregations)
}

package com.emnify.infrastructure

import cats.effect.kernel.Sync
import cats.implicits._
import cats.{MonadThrow, ~>}
import com.emnify.common.Fail
import com.emnify.domain.{AggregationRepository, AggregationService, ProductCode, ProductOfferAggregation}

object AggregationService {

  def apply[F[_]: MonadThrow, G[_]: Sync](
      aggregationRepository: AggregationRepository[G],
      defaultCloseOfferAggregationThreshold: Long,
      liftF: G ~> F
  ): AggregationService[F] =
    new AggregationService[F] {

      override type GG[X] = G[X]

      def closeAggregation(
          code: ProductCode,
          closeAggregationThreshold: Option[Long]
      ): F[ProductCode] = {

        val result = aggregationRepository.getAggregation(code).flatMap {
          case Some(aggr) =>
            val closingAggregationThreshold = closeAggregationThreshold.getOrElse(defaultCloseOfferAggregationThreshold)
            if (aggr.noOfOffers < closingAggregationThreshold) {
              Fail
                .IncorrectInput(
                  s"Error during closing aggregation with the code [$code]. Number of offers must be greater than or equal to $closingAggregationThreshold"
                )
                .raiseError[G, ProductCode]
            } else
              aggregationRepository.closeAggregation(code)

          case None => Fail.NotFound(s"Aggregation with the code [$code] not found").raiseError[G, ProductCode]
        }

        gToF(result)
      }

      override def getAggregation(code: ProductCode): F[ProductOfferAggregation] =
        gToF(for {
          aggrOpt <- aggregationRepository.getAggregation(code)
          aggr <- aggrOpt.liftTo[G](Fail.NotFound(s"Aggregation with a code [$code] not found"))
        } yield aggr)

      override def insertOrUpdateAggregations(offers: List[ProductOfferAggregation]): F[Int] =
        gToF(aggregationRepository.insertOrUpdateAggregations(offers))

      override def gToF: GG ~> F = liftF
    }
}

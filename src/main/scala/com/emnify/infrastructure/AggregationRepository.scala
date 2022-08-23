package com.emnify.infrastructure

import cats.data.NonEmptyList
import cats.free.Free
import cats.implicits._
import com.emnify.domain.{AggregationRepository, AggregationStatus, AvgPrice, MaxPrice, MinPrice, ProductCode, ProductOfferAggregation}
import doobie.free.connection
import doobie.implicits._
import doobie.util.meta.Meta
import doobie.util.update.Update
import doobie.{ConnectionIO, Fragments}
import com.emnify.common.Doobie._

object AggregationRepository {

  val aggregationStateToInt: AggregationStatus => Long = {
    case AggregationStatus.Open   => 1
    case AggregationStatus.Closed => 0
  }
  val intToAggregationState: Long => AggregationStatus = {
    case 1 => AggregationStatus.Open
    case 0 => AggregationStatus.Closed
  }

  implicit val aggregationStateMeta: Meta[AggregationStatus] = Meta[Long].imap(intToAggregationState)(aggregationStateToInt)

  def apply(): AggregationRepository[ConnectionIO] = new AggregationRepository[ConnectionIO] {

    def closeAggregation(code: ProductCode): ConnectionIO[ProductCode] =
      sql"""UPDATE PRODUCT_OFFER_AGGREGATION set state = 0 WHERE PRODUCT_CODE = ${code.toString}""".update.run.as(code)

    def getAggregation(productCode: ProductCode): ConnectionIO[Option[ProductOfferAggregation]] = {
      sql"""SELECT 
                | product_code, 
                | min_price, 
                | max_price, 
                | avg_price,
                | no_of_offers,
                | state
                | FROM PRODUCT_OFFER_AGGREGATION 
                | WHERE product_code = ${productCode.toString}""".stripMargin
        .query[ProductOfferAggregation]
        .option
    }

    private def selectManyToUpdate(codes: NonEmptyList[String]) =
      (fr"""
          SELECT product_code, min_price, max_price, avg_price, no_of_offers, state FROM PRODUCT_OFFER_AGGREGATION WHERE """ ++ Fragments
        .in(fr"product_code", codes))
        .query[ProductOfferAggregation]
        .to[List]

    def insertOrUpdateAggregations(aggregations: List[ProductOfferAggregation]): ConnectionIO[Int] = {

      val codesToUpdate = aggregations.map(_.productCode)

      val selectedToUpdate = NonEmptyList.fromList(codesToUpdate) match {
        case Some(elems) => selectManyToUpdate(elems)
        case None        => List.empty[ProductOfferAggregation].pure[ConnectionIO]
      }

      val aggregationsToInsert: Free[connection.ConnectionOp, List[ProductOfferAggregation]] = selectedToUpdate.map { aggrsFromDB =>
        val codesFromDB = aggrsFromDB.map(_.productCode)
        aggregations.collect {
          case elem if !codesFromDB.contains(elem.productCode) => elem
        }
      }

      val aggregationsToUpdate = selectedToUpdate
        .map { aggrsFromDB =>
          (aggrsFromDB.collect {
            case elem if codesToUpdate.contains(elem.productCode) && elem.status == AggregationStatus.Open => elem
          } ++ aggregations.collect {
            case elem if aggrsFromDB.exists(_.status == AggregationStatus.Open) => elem
          })
            .groupMapReduce(_.productCode)(identity)((a, b) =>
              ProductOfferAggregation(
                a.productCode,
                MinPrice(a.minPrice + b.minPrice),
                MaxPrice(a.maxPrice + b.maxPrice),
                AvgPrice(a.avgPrice + b.avgPrice),
                a.noOfOffers + b.noOfOffers,
                AggregationStatus.Open
              )
            )
            .values
            .toList
        }

      for {
        toUpdate <- aggregationsToUpdate
        _ <- updateAggregations(toUpdate)
        toInsert <- aggregationsToInsert
        _ <- insertAggregations(toInsert)
      } yield toUpdate.size
    }

    def insertAggregations(aggregations: List[ProductOfferAggregation]): ConnectionIO[Int] = {
      val query =
        """INSERT INTO PRODUCT_OFFER_AGGREGATION (
          |    product_code,
          |    min_price,
          |    max_price,
          |    avg_price,
          |    no_of_offers,
          |    state
          | ) VALUES (?, ?, ?, ?, ?, ?)        
           """.stripMargin

      Update[ProductOfferAggregation](query).updateMany(aggregations)
    }

    def updateAggregations(offers: List[ProductOfferAggregation]): ConnectionIO[Int] = {
      val query =
        """
          |UPDATE PRODUCT_OFFER_AGGREGATION SET 
          | min_price = ?, 
          | max_price = ?, 
          | avg_price = ?, 
          | no_of_offers = ?
          | WHERE product_code = ?
          |""".stripMargin

      case class UpdateAggregation(
          minPrice: BigDecimal,
          maxPrice: BigDecimal,
          avgPrice: BigDecimal,
          noOfOffers: Long,
          productCode: String
      )

      val records = offers.map(offer =>
        UpdateAggregation(
          offer.minPrice,
          offer.maxPrice,
          offer.avgPrice,
          offer.noOfOffers,
          offer.productCode
        )
      )

      Update[UpdateAggregation](query).updateMany(records)
    }
  }
}

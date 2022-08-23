package com.emnify

import com.softwaremill.tagging.{@@, Tagger}

import scala.math.BigDecimal.RoundingMode

package object domain {

  type ProductCodeTag
  type ProductCode = String @@ ProductCodeTag
  object ProductCode extends Tagger[String, ProductCodeTag]

  type PriceTag
  type Price = BigDecimal @@ PriceTag
  object Price extends Tagger[BigDecimal, PriceTag] {
    val Zero: Price = Price(0.0)

    implicit class PriceTagOps(price: Price) {
      def roundPrice: Price = Price(price.setScale(2, RoundingMode.HALF_UP))
    }
  }

  type MinPrice = BigDecimal @@ PriceTag
  object MinPrice extends Tagger[BigDecimal, PriceTag] {
    val Zero: MinPrice = MinPrice(0.0)

    implicit class PriceTagOps(price: Price) {
      def roundPrice: Price = Price(price.setScale(2, RoundingMode.HALF_UP))
    }
  }

  type MaxPrice = BigDecimal @@ PriceTag
  object MaxPrice extends Tagger[BigDecimal, PriceTag] {
    val Zero: MaxPrice = MaxPrice(0.0)

    implicit class PriceTagOps(price: Price) {
      def roundPrice: MaxPrice = MaxPrice(price.setScale(2, RoundingMode.HALF_UP))
    }
  }

  type AvgPrice = BigDecimal @@ PriceTag
  object AvgPrice extends Tagger[BigDecimal, PriceTag] {
    val Zero: AvgPrice = AvgPrice(0.0)

    implicit class PriceTagOps(price: Price) {
      def roundPrice: Price = Price(price.setScale(2, RoundingMode.HALF_UP))
    }
  }

  class Tagger[V, T] {
    def apply(value: V): V @@ T = value.taggedWith[T]
    def unapply(value: V): Option[V] = Some(value)
  }
}

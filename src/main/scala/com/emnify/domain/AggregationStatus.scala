package com.emnify.domain

import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema
trait AggregationStatus

object AggregationStatus {

  case object Open extends AggregationStatus
  case object Closed extends AggregationStatus

  implicit lazy val schemaAggregation: Schema[AggregationStatus] = Schema.string

  implicit val productCodeEncoder: Encoder[AggregationStatus] = Encoder[String].contramap {
    case Open   => "Open"
    case Closed => "Closed"
  }

  implicit val productCodeDecoder: Decoder[AggregationStatus] = Decoder[String].emap {
    case "Open"   => Right(Open)
    case "Closed" => Right(Closed)
  }
}

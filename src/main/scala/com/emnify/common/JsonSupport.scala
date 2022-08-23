package com.emnify.common

import com.softwaremill.tagging.@@
import io.circe.generic.AutoDerivation
import io.circe.{Decoder, Encoder, Printer}
import sttp.tapir.Tapir
import sttp.tapir.json.circe.TapirJsonCirce

trait JsonSupport extends AutoDerivation with Tapir with TapirJsonCirce {

  val noNullsPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)

  implicit def taggedStringEncoder[U]: Encoder[String @@ U] = Encoder.encodeString.asInstanceOf[Encoder[String @@ U]]
  implicit def taggedStringDecoder[U]: Decoder[String @@ U] = Decoder.decodeString.asInstanceOf[Decoder[String @@ U]]

  implicit def taggedBigDecimalEncoder[U]: Encoder[BigDecimal @@ U] = Encoder.encodeBigDecimal.asInstanceOf[Encoder[BigDecimal @@ U]]
  implicit def taggedBigDecimalDecoder[U]: Decoder[BigDecimal @@ U] = Decoder.decodeBigDecimal.asInstanceOf[Decoder[BigDecimal @@ U]]
}

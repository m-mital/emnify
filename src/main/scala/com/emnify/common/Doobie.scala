package com.emnify.common

import com.softwaremill.tagging.@@
import doobie.util.meta.Meta
import scala.reflect.runtime.universe.TypeTag

object Doobie {

  implicit def taggedStringMeta[U: TypeTag]: Meta[String @@ U] =
    Meta[String].asInstanceOf[Meta[String @@ U]]

  implicit def taggedBigDecimalMeta[U: TypeTag]: Meta[BigDecimal @@ U] =
    Meta[BigDecimal].asInstanceOf[Meta[BigDecimal @@ U]]

  implicit def taggedLongMeta[U: TypeTag]: Meta[Long @@ U] =
    Meta[Long].asInstanceOf[Meta[Long @@ U]]

}

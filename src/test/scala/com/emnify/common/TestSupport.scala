package com.emnify.common

import cats.effect.IO

import scala.concurrent.duration._
import cats.effect.unsafe.implicits.global
import io.circe.{Decoder, parser}

import scala.reflect.ClassTag

trait TestSupport {

  implicit class RichEither(r: Either[String, String]) {
    def shouldDeserializeTo[T: Decoder: ClassTag]: T =
      r.flatMap(parser.parse).flatMap(_.as[T]).right.get
  }

  implicit class RichIO[T](t: IO[T]) {
    def unwrap: T = t.unsafeRunTimed(1.minute).get
  }
}

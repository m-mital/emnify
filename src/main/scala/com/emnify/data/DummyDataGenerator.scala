package com.emnify.data

import cats.effect.IO
import com.emnify.domain.{Offer, Price, ProductCode}
import fs2.{Pure, Stream}

import scala.concurrent.duration._

class DummyDataGenerator() extends DataSource[IO] {

  private val dataStream: Stream[Pure, (String, BigDecimal)] = Stream(
    ("vacuum-cleaner", BigDecimal(209.99)),
    ("wash-machine", BigDecimal(1999.99)),
    ("dish-machine", BigDecimal(2000)),
    ("dish-machine", BigDecimal(3000))
  ).repeatN(3)

  private val ticksStream: Stream[IO, FiniteDuration] = Stream.awakeEvery[IO](100.millis)

  val source: Stream[IO, Offer] = ticksStream.zipRight(dataStream).flatMap { case (s, p) =>
    Stream
      .eval(IO.realTime)
      .map(_.toMillis)
      .map(_ => Offer(ProductCode(s), Price(p)))
  }
}

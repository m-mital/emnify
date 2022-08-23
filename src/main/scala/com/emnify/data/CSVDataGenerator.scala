package com.emnify.data

import cats.effect.IO
import com.emnify.domain.{Offer, Price, ProductCode}
import fs2.io.file.{Files, Path}
import fs2.{Pipe, Stream, text}
import log.effect.LogWriter

import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.Try

class CSVDataGenerator(filename: String, timeAggregationRate: Long)(implicit log: LogWriter[IO]) extends DataSource[IO] {

  private def parseLine[F[_]]: Pipe[F, List[String], Option[Offer]] =
    _.map {
      case _ :: price :: _ :: _ :: productCode :: Nil =>
        Try {
          Offer(ProductCode(productCode), Price(BigDecimal(price)))
        }.toOption
      case _ => None
    }

  private def parser[F[_]]: Pipe[F, Byte, List[String]] =
    _.through(text.utf8.decode)
      .through(text.lines)
      .map(_.split(",").toList.take(5))

  private def csvParser: Stream[IO, Offer] =
    Files[IO]
      .readAll(Path(filename))
      .through(parser)
      .through(parseLine)
      .unNone

  private val ticksStream: Stream[IO, FiniteDuration] = Stream.awakeEvery[IO](timeAggregationRate.millis)

  val source: fs2.Stream[IO, Offer] =
    ticksStream
      .zipRight(csvParser)
      .flatMap(offer => Stream.emit(offer))
}

package com.emnify.data

import com.emnify.domain.Offer
import fs2.Stream

trait DataSource[F[_]] {
  val source: Stream[F, Offer]
}

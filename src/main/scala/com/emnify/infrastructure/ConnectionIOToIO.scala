package com.emnify.infrastructure

import cats.arrow.FunctionK
import cats.effect.IO
import cats.~>
import doobie.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor

object ConnectionIOToIO {

  def apply(transactor: Transactor[IO]): ConnectionIO ~> IO = FunctionK.liftFunction(_.transact(transactor))
}

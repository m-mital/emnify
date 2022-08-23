package com.emnify.common

import cats.effect.IO
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import com.emnify.infrastructure.config.DbConfig
import doobie.Transactor
import doobie.h2.H2Transactor
import doobie.implicits._
import org.flywaydb.core.Flyway

import scala.annotation.tailrec
import scala.concurrent.duration._

class TestDB(config: DbConfig, migrationsDirs: Seq[String]) {

  var xa: Transactor[IO] = _
  private val xaReady: Queue[IO, Transactor[IO]] = Queue.unbounded[IO, Transactor[IO]].unsafeRunSync()
  private val done: Queue[IO, Unit] = Queue.unbounded[IO, Unit].unsafeRunSync()

  {
    val xaResource = for {
      connectEC <- doobie.util.ExecutionContexts.fixedThreadPool[IO](32)
      xa <- H2Transactor.newH2Transactor[IO](
        s"jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;",
        config.username,
        config.password,
        connectEC
      )
    } yield xa

    // first extracting it from the use method, then stopping when the `done` mvar is filled (when `close()` is invoked)
    xaResource
      .use { _xa =>
        xaReady.offer(_xa) >> done.take
      }
      .start
      .unsafeRunSync()

    xa = xaReady.take.unsafeRunSync()
  }

  @tailrec
  final def connectAndMigrate(): Unit = {
    try {
      migrate()
      testConnection()
    } catch {
      case e: Exception =>
        Thread.sleep(5000)
        connectAndMigrate()
    }
  }
  private val flyway = {
    Flyway
      .configure()
      .locations(migrationsDirs: _*)
      .cleanDisabled(false)
      .dataSource(config.url, config.username, config.password)
      .load()
  }

  def testConnection(): Unit = {
    sql"select 1".query[Int].unique.transact(xa).unsafeRunTimed(1.minute)
    ()
  }
  def migrate(): Unit = {
    if (config.migrateOnStart) {
      flyway.migrate()
      ()
    }
  }

  def clean(): Unit = {
    flyway.clean()
  }

  def close(): Unit = {
    done.offer(()).unsafeRunTimed(1.minute)
  }
}

package com.emnify.common

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.emnify.infrastructure.ConnectionIOToIO
import com.emnify.infrastructure.config.DbConfig
import doobie.ConnectionIO
import doobie.syntax.ConnectionIOOps
import doobie.util.transactor.Transactor
import log.effect.fs2.SyncLogWriter
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

import scala.concurrent.duration._

trait BasicDbTestSupport extends BeforeAndAfterEach with BeforeAndAfterAll {
  self: Suite =>

  def config = DbConfig(username = "sa", password = "", url = "jdbc:h2:mem:test;MODE=PostgreSQL;", migrateOnStart = true)

  implicit val logWriter = SyncLogWriter.consoleLog[IO]

  protected var db: TestDB = _
  protected var xa: Transactor[IO] = _

  protected def migrationDirs: Seq[String] = Seq("filesystem:src/main/resources/db/migration")

  private def initDB(): Unit = {
    db = new TestDB(config, migrationDirs)
    xa = db.xa
    db.testConnection()
  }

  initDB()

  val gToF = ConnectionIOToIO(xa)

  implicit class RichConnectionIO[T](t: ConnectionIO[T]) {
    def unwrap(transactor: Transactor[IO]): T =
      new ConnectionIOOps(t).transact(transactor).unsafeRunTimed(2.minute).get
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    db.clean()
    db.migrate()
  }

  override protected def afterAll(): Unit = {
    db.clean()
    super.afterAll()
  }
}

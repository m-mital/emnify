package com.emnify.infrastructure

import cats.effect.{Async, IO, Resource, Sync}
import doobie.h2.H2Transactor
import doobie.util.ExecutionContexts
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.apply._
import com.emnify.infrastructure.config.DbConfig
import doobie._
import doobie.implicits._
import log.effect.LogWriter
import org.flywaydb.core.Flyway

import scala.concurrent.duration._

abstract class TransactorResource[F[_]: Async](config: DbConfig)(implicit log: LogWriter[F]) {

  def transactor: Resource[F, H2Transactor[F]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](32)
      xa <- H2Transactor.newH2Transactor[F](
        s"${config.url};DB_CLOSE_DELAY=-1",
        config.username,
        config.password,
        ce
      )
      _ <- Resource.eval(connect(xa))
    } yield xa

  private def connect(xa: H2Transactor[F]): F[Unit] = {
    (testConnection(xa) *> log.debug("Connection test complete")).recoverWith { case e =>
      log.warn("Database not available, waiting 5 seconds to retry...", e)
      Sync[F].wait(5 * 1000).pure[F] *> connect(xa)
    }
  }
  protected def testConnection(xa: Transactor[F]): F[Int] =
    sql"select 1".query[Int].unique.transact(xa)

}
class Database[F[_]: Async](config: DbConfig)(implicit log: LogWriter[IO]) extends TransactorResource[IO](config) {

  def connectAndMigrate(xa: H2Transactor[IO]): IO[Unit] = {
    (migrate() >> testConnection(xa) >>
      log.info("Database migration & connection test complete")).onError { e =>
      log.warn("Database not available, waiting 5 seconds to retry...", e)
      IO.sleep(5.seconds) >> connectAndMigrate(xa)
    }
  }

  private val flyway = {
    Flyway
      .configure()
      .locations("filesystem:src/main/resources/db/migration")
      .dataSource(config.url, config.username, config.password)
      .load()
  }

  private def migrate(): IO[Unit] = {
    if (config.migrateOnStart) {
      IO(flyway.migrate())
    } else IO.unit
  }
}

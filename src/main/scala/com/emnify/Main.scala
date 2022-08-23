package com.emnify

import cats.effect._
import com.emnify.data.CSVDataGenerator
import com.emnify.http.HttpApi
import com.emnify.infrastructure.config.Config
import com.emnify.infrastructure._
import doobie.ConnectionIO
import log.effect.fs2.SyncLogWriter
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Server
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContext

object Main extends ResourceApp.Forever {

  implicit val logWriter = SyncLogWriter.consoleLog[IO]

  private def httpServer[F[_]: Async](routes: HttpRoutes[F]): Resource[F, Server] = BlazeServerBuilder[F]
    .withExecutionContext(ExecutionContext.global)
    .bindHttp()
    .withHttpApp(routes.orNotFound)
    .resource

  override def run(args: List[String]): Resource[IO, Unit] = for {
    config <- Resource.eval(IO(ConfigSource.default.loadOrThrow[Config]))
    db = new Database[IO](config.db)
    transactor <- db.transactor
    _ <- Resource.eval(db.connectAndMigrate(transactor))
    runConnectionIOToIO = ConnectionIOToIO(transactor)
    repository = AggregationRepository()
    aggregationService = AggregationService[IO, ConnectionIO](repository, config.closeOfferAggregationThreshold, runConnectionIOToIO)
    routes = new HttpApi(aggregationService).routes
    dataSource = new CSVDataGenerator(config.dataSource, config.timeAggregationRate)
    _ <- httpServer[IO](routes)
    _ <- Resource.eval(new ProductOfferAggregationProgram[IO](dataSource, aggregationService).program.compile.drain)
  } yield ()
}

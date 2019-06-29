package elevio.article

import java.util.concurrent.Executors

import cats.effect.{IO, Resource, _}
import doobie.hikari.HikariTransactor

import scala.concurrent.ExecutionContext
import cats.implicits._
import com.itv.bucky.test.AmqpClientTest
import org.http4s.client.Client
import org.http4s.implicits._
import elevio.article.app.{App, Config}
import doobie.util.transactor.Transactor

package object it {

/*  def withApp[A](testData: TestData = TestData.empty, amqp: Option[AmqpClientTest[IO]] = None)(test: (TestApp, TestData) => IO[A]): A =
    (for {
      conf     <- Resource.make(Main.config.toIO)(_ => IO.unit)
      executor <- Resource.make(IO(Executors.newFixedThreadPool(10)))(e => IO(e.shutdown()))
      ec    = ExecutionContext.fromExecutor(executor)
      timer = IO.timer(ec)
      cs    = IO.contextShift(ec)
      amqpClient     <- AmqpClientTest[IO](IO.ioConcurrentEffect(cs), timer, cs, ec).clientStrict()
      dbConnectionEc <- app.dbConnectionThreadPool(conf).map(ExecutionContext.fromExecutor)
      dbTransactEc   <- app.dbConnectionThreadPool(conf).map(ExecutionContext.fromExecutor)
      client         <- app.httpClient(conf)(ec, cs)
      transactor <- HikariTransactor
        .newHikariTransactor[IO](conf.db.driverClassName, conf.db.url, conf.db.user, conf.db.pass, dbConnectionEc, dbTransactEc)(
          IO.ioConcurrentEffect(cs),
          cs)
    } yield {
      App(ec, cs, timer, transactor, client, amqpClient, conf)
    }).use(app => {
        val testApp = new TestApp {
          override def db: Transactor[IO]       = app.db
          override def amqp: AmqpClientTest[IO] = app.amqp.asInstanceOf[AmqpClientTest[IO]] //meh..who cares
          override def config: Config        = app.config
          override def appClient: Client[IO]    = Client.fromHttpApp(app.routes.orNotFound)
        }
        test(
          testApp,
          testData
        )
      })
      .unsafeRunSync()*/
}

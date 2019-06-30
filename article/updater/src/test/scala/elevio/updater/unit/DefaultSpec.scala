package elevio.updater.unit

import java.util.concurrent.Executors

import cats.effect.{ContextShift, IO, Resource, Timer}
import cats.implicits._
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.ExecutionContext

trait DefaultSpec extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockitoSugar {
  def async(testFun: (ExecutionContext, ContextShift[IO], Timer[IO]) => IO[_]): Unit =
    Resource
      .make(IO(Executors.newFixedThreadPool(10)))(ec => IO(ec.shutdown()))
      .map(ExecutionContext.fromExecutor)
      .use { ec =>
        val timer = IO.timer(ec)
        val cs    = IO.contextShift(ec)
        (IO.shift(ec) *> testFun(ec, cs, timer)).void
      }
      .unsafeRunSync()

  def withIO[A](test: IO[A]): A =
    test.unsafeRunSync()

  def withResources[A](test: ContextShift[IO] => IO[A]): A =
    withIO {
      (for {
        executor <- Resource.make(IO(Executors.newFixedThreadPool(10)))(ec => IO(ec.shutdown()))
      } yield ExecutionContext.fromExecutor(executor))
        .use { ec =>
          test(IO.contextShift(ec))
        }
    }
}

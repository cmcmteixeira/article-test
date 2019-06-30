package elevio.updater

import cats.effect.{ExitCode, IO, IOApp, Resource, SyncIO}

import scala.concurrent.ExecutionContext
import cats.implicits._
import cats.effect._
import com.itv.bucky.publish._
import com.itv.bucky.circe._
import com.itv.bucky.AmqpClient
import elevio.common.httpclient.ElevioArticleClient
import elevio.common.model.ArticleUpdate
import elevio.updater.httpclients.InternalArticlesClient
import elevio.updater.services.{ElevioWalker, InternalWalker}

import scala.concurrent.duration._
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
class Main extends IOApp.WithContext {
  override protected def executionContextResource: Resource[SyncIO, ExecutionContext] = ???

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val ec: ExecutionContext = executionContext
    (for {
      amqp       <- AmqpClient[IO](???)
      httpClient <- BlazeClientBuilder[IO](executionContext).resource
      internalClient = InternalArticlesClient(httpClient, ???)
      elevioClient   = ElevioArticleClient(httpClient, ???)
      publisher      = amqp.publisherOf[ArticleUpdate](???, ???)
      elevioWalker   = ElevioWalker(???, elevioClient, internalClient, publisher)
      internalWalker = InternalWalker(???, elevioClient, internalClient, publisher)

    } yield (elevioWalker, internalWalker)).use {
      case (elevio, internal) =>
        (for {

          _ <- Stream.awakeDelay[IO](2.seconds)
          _ <- Stream.eval(elevio.run)
          _ <- Stream.eval(internal.run)
        } yield ()).compile.drain *> IO(ExitCode.Success)
    }

  }

}

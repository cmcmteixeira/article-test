package elevio.updater.services

import cats.effect.{ContextShift, IO, Timer}
import com.itv.bucky.Publisher
import elevio.common.httpclient.ElevioArticleClient
import elevio.common.model.{ArticleUpdate, ItemsPerPage, Page}
import elevio.updater.httpclients.InternalArticlesClient
import fs2.Stream

import scala.concurrent.duration.FiniteDuration

trait Walker {
  def run: IO[Unit]
}

object ElevioWalker {
  case class ElevioWalkerConfig(parallelization: Int, rate: FiniteDuration)
  def apply(
      config: ElevioWalkerConfig,
      elevioClient: ElevioArticleClient,
      internalArticlesClient: InternalArticlesClient,
      updatePublisher: Publisher[IO, ArticleUpdate]
  )(implicit cs: ContextShift[IO], t: Timer[IO]): Walker = new Walker {
    override def run: IO[Unit] =
      Stream
        .iterate[IO, Page](Page(1L))(p => Page(p.value + 1L))
        .metered(config.rate)
        .evalMap(elevioClient.fetchPaginatedArticleList)
        .takeThrough(p => p.page.value < p.totalPages.value)
        .flatMap(page => Stream.emits(page.elements))
        .parEvalMap(config.parallelization)(details => {
          for {
            internalArticle <- internalArticlesClient.fetchInternalArticle(details.id)
          } yield (details, internalArticle)
        })
        .filter {
          case (elevio, internal) => !internal.contains(elevio)
        }
        .map(_._1)
        .parEvalMap(config.parallelization)(article => updatePublisher(ArticleUpdate(article.id)))
        .compile
      .drain
  }
}

object InternalWalker {
  case class InternalWalkerConfig(parallelization: Int, itemsPerPage: ItemsPerPage, rate: FiniteDuration)

  def apply(
      config: InternalWalkerConfig,
      evelioClient: ElevioArticleClient,
      internalArticlesClient: InternalArticlesClient,
      updatePublisher: Publisher[IO, ArticleUpdate]
  )(implicit cs: ContextShift[IO], t: Timer[IO]): Walker = new Walker {
    override def run: IO[Unit] =
      Stream
        .iterate[IO, Page](Page(1L))(p => Page(p.value + 1L))
        .evalMap(internalArticlesClient.fetchInternalArticles(_, config.itemsPerPage))
        .takeThrough(p => p.page.value < p.totalPages.value)
        .flatMap(page => Stream.emits(page.elements))
        .metered(config.rate)
        .parEvalMap(config.parallelization)(internal =>
          for {
            elevio <- evelioClient.fetchArticle(internal.id)
          } yield (elevio, internal))
        .filter {
          case (elevio, internal) => !elevio.map(_.toArticle).contains(internal)
        }
        .map(_._2)
        .parEvalMap(config.parallelization)(article => updatePublisher(ArticleUpdate(article.id)))
        .compile
        .drain
  }
}

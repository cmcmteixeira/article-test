package elevio.service.services

import cats.data.{NonEmptyList, OptionT}
import cats.effect.IO
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import elevio.common.httpclient.ElevioArticleClient
import elevio.common.model._
import elevio.service.repository.ArticleRepository
import elevio.service.services.ArticleService.ArticleFilter

trait ArticleService {
  def getArticleDetails(articleId: ArticleId): IO[Option[ArticleDetails]]
  def fetchArticlesPaginated(page: Page, filter: ArticleFilter): IO[ServicePaginatedList[Article]]
  def updateInternalArticleDetails(articleId: ArticleId): IO[Unit]
  def getInternalArticle(article: ArticleId): IO[Option[Article]]
  def getInternalArticles(page: Page, itemsPerPage: ItemsPerPage): IO[ServicePaginatedList[Article]]
}

object ArticleService {
  case class ArticleServiceConfig(itemsPerPage: ItemsPerPage)
  case class ArticleFilter(keywords: Option[NonEmptyList[ArticleKeyWord]] = None)
  type IOOptionT[A] = OptionT[IO, A]
  def apply(
      articleClient: ElevioArticleClient,
      articleRepository: ArticleRepository,
      config: ArticleServiceConfig
  ): ArticleService = new ArticleService with StrictLogging {

    override def getArticleDetails(articleId: ArticleId): IO[Option[ArticleDetails]] =
      (for {
        _      <- IO(logger.info(s"Fetching article $articleId."))
        result <- articleClient.fetchArticle(articleId)
        _      <- IO(logger.info(s"Found ${result.size} article(s)."))
      } yield result).attempt.flatTap {
        case Left(e)  => IO(logger.error("Failed to obtain article.", e))
        case Right(v) => IO(logger.info(s"Found ${v.size} articles."))
      }.rethrow

    override def fetchArticlesPaginated(page: Page, filter: ArticleFilter): IO[ServicePaginatedList[Article]] =
      for {
        page <- filter.keywords.fold(articleClient.fetchPaginatedArticleList(page).map(_.toServicePaginatedList))(_ =>
          articleRepository.findArticlesFor(page, config.itemsPerPage)(filter))
      } yield page

    override def getInternalArticle(article: ArticleId): IO[Option[Article]] =
      for {
        _       <- IO(logger.info("Querying for articles."))
        article <- articleRepository.findArticleFor(article)
        _       <- IO(logger.info(s"Found ${article.size} article(s)."))
      } yield article

    override def updateInternalArticleDetails(articleId: ArticleId): IO[Unit] =
      for {
        _       <- IO(logger.info(s"Updating $articleId"))
        article <- articleClient.fetchArticle(articleId)
        _       <- IO(logger.info(s"${article.fold("Will delete ")(_ => "Will update ")} article $articleId"))
        _       <- article.fold(articleRepository.deleteArticle(articleId))(a => articleRepository.upsertArticle(a.toArticle))
        _       <- IO(logger.info(s"Successfully updated $articleId"))
      } yield ()

    override def getInternalArticles(page: Page, itemsPerPage: ItemsPerPage): IO[ServicePaginatedList[Article]] =
      for {
        _        <- IO(logger.info(s"Querying internal state for articles with $page and $itemsPerPage"))
        articles <- articleRepository.findArticlesFor(page, itemsPerPage)(ArticleFilter())
        _        <- IO(logger.info(s"Found ${articles.pageSize} article(s)."))
      } yield articles
  }
}

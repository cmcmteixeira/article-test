package elevio.updater.httpclients

import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import elevio.common.model.{Article, ArticleId, ItemsPerPage, Page, PaginatedList, ServicePaginatedList}
import org.http4s.Uri
import org.http4s.client.Client
import elevio.common.httpclient._

trait InternalArticlesClient {
  def fetchInternalArticles(page: Page, itemsPerPage: ItemsPerPage): IO[ServicePaginatedList[Article]]
  def fetchInternalArticle(articleId: ArticleId): IO[Option[Article]]
}

object InternalArticlesClient {
  case class InternalArticleClientConfig(baseUrl: Uri)
  def apply(client: Client[IO], conf: InternalArticleClientConfig): InternalArticlesClient = new InternalArticlesClient with StrictLogging {
    override def fetchInternalArticles(page: Page, itemsPerPage: ItemsPerPage): IO[ServicePaginatedList[Article]] = {
      val url = conf.baseUrl.withPath("/internal/articles").withQueryParam("itemsPerPage", itemsPerPage.value).withQueryParam("page", page.value)
      for {
        _        <- IO(logger.info(s"Fetching  $itemsPerPage  articles for page $page."))
        articles <- client.expect[ServicePaginatedList[Article]](url)
        _        <- IO(logger.info(s"Found  ${articles.elements.size}  articles."))
      } yield articles
    }

    override def fetchInternalArticle(articleId: ArticleId): IO[Option[Article]] =
      for {
        _       <- IO(logger.info(s"Fetching  internal $articleId."))
        article <- client.get(conf.baseUrl.withPath(s"/internal/articles/${articleId.value}"))(handle404[Article])
        _       <- IO(logger.info(s"Found ${article.size} article(s)."))
      } yield article
  }
}

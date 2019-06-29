package elevio.article.httpclient

import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import elevio.article.model.{ApiKey, Article, ArticleId, ElevioPaginatedList, JWT, Page}
import org.http4s.{Uri, _}
import org.http4s.client.Client
import org.http4s.dsl.io._

trait ArticleClient {
  def fetchArticle(articleId: ArticleId): IO[Option[Article]]
  def fetchPaginatedArticleList(page: Page): IO[ElevioPaginatedList[Article]]
}

object ArticleClient {
  case class ArticleClientConfig(basePath: Uri, version: String, jwtToken: JWT, apiKey: ApiKey)
  def apply(client: Client[IO], config: ArticleClientConfig): ArticleClient = new ArticleClient with StrictLogging {
    override def fetchArticle(articleId: ArticleId): IO[Option[Article]] =
      for {
        _        <- IO(logger.info(s"Fetching article by id ${articleId.value}"))
        url      <- IO.pure(config.basePath / config.version / s"articles" / articleId.value.toString)
        request  <- IO(Request[IO](method = GET, uri = url).withAuthentication(config.jwtToken, config.apiKey))
        response <- client.fetch(request)(handle404[Article])
        _        <- response.fold(IO(logger.warn("Article was not found!")))(_ => IO(logger.info("Successfully retrieved article!")))
      } yield response

    override def fetchPaginatedArticleList(page: Page): IO[ElevioPaginatedList[Article]] =
      for {
        _        <- IO(logger.info(s"Fetching paginated list of articles $page"))
        url      <- IO.pure((config.basePath / config.version / s"articles").withQueryParam("page", page.value))
        request  <- IO(Request[IO](method = GET, uri = url).withAuthentication(config.jwtToken, config.apiKey))
        response <- client.fetch(request)(_.as[ElevioPaginatedList[Article]])
        _        <- IO(logger.info(s"Fetched paginated list of articles. Retrieved $response"))
      } yield response
  }

}

package elevio.common.httpclient

import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import elevio.common.model.{ApiKey, Article, ArticleDetails, ArticleId, ElevioPaginatedList, JWT, Page}
import org.http4s.{Uri, _}
import org.http4s.client.Client
import org.http4s.dsl.io._

trait ElevioArticleClient {
  def fetchArticle(articleId: ArticleId): IO[Option[ArticleDetails]]
  def fetchPaginatedArticleList(page: Page): IO[ElevioPaginatedList[Article]]
}

object ElevioArticleClient {

  case class ElevioArticleClientConfig(basePath: Uri, version: String, jwtToken: JWT, apiKey: ApiKey)

  def apply(client: Client[IO], config: ElevioArticleClientConfig): ElevioArticleClient = new ElevioArticleClient with StrictLogging {
    override def fetchArticle(articleId: ArticleId): IO[Option[ArticleDetails]] =
      for {
        _        <- IO(logger.info(s"Fetching article by id ${articleId.value}"))
        url      <- IO.pure(config.basePath / config.version / s"articles" / articleId.value.toString)
        request  <- IO(Request[IO](method = GET, uri = url).withAuthentication(config.jwtToken, config.apiKey))
        response <- client.fetch(request)(handle404[ArticleDetails])
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

package elevio.article.routes

import cats.data.NonEmptyList
import cats.effect.IO
import elevio.article.model.{Article, ArticleId, KeyWord, Page}
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl.io._
import org.http4s.QueryParamDecoder._
import org.http4s.dsl.impl.IntVar
import cats.implicits._
import elevio.article.services.ArticleService
import elevio.article.services.ArticleService.ArticleFilter

class ArticleRoutes(articleService: ArticleService) {
   object KeyWordParamMatcher extends OptionalMultiQueryParamDecoderMatcher[KeyWord]("keyword")

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root :? KeyWordParamMatcher(keywords) +& PageParamMatcher(pageOpt) =>
      (for {
        page          <- pageOpt.fold(IO.raiseError[Page](BadRequestE("Please provide a page.")))(IO.pure[Page])
        validKeyWords <- keywords.fold(_ => IO.raiseError(BadRequestE("Invalid keywords.")), IO.pure)
        articles      <- articleService.fetchPaginatedArticleList(page, ArticleFilter(NonEmptyList.fromList(validKeyWords)))
        response      <- Ok(articles)
      } yield response).recoverWith(defaultExceptionHandler)
    case GET -> Root / IntVar(articleId) =>
      (for {
        articleOpt <- articleService.getArticleDetails(ArticleId(articleId))
        article    <- articleOpt.fold(IO.raiseError[Article](NotFoundE()))(_.pure[IO])
        response   <- Ok(article)
      } yield response).recoverWith(defaultExceptionHandler)
  }
}

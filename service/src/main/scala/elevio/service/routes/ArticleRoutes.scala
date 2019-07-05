package elevio.service.routes

import cats.data.NonEmptyList
import cats.effect.IO
import cats.implicits._
import elevio.common.model.{Article, ArticleDetails, ArticleId, ArticleKeyWord, Page}
import elevio.service.services.ArticleService
import elevio.service.services.ArticleService.ArticleFilter
import org.http4s.HttpRoutes
import org.http4s.dsl.impl.IntVar
import org.http4s.dsl.io._

class ArticleRoutes(articleService: ArticleService) {
  object KeyWordParamMatcher extends OptionalMultiQueryParamDecoderMatcher[ArticleKeyWord]("keyword")

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root :? KeyWordParamMatcher(keywords) +& PageParamMatcher(pageOpt) =>
      (for {
        page          <- pageOpt.fold(IO.raiseError[Page](BadRequestE("Please provide a page.")))(IO.pure[Page])
        validKeyWords <- keywords.fold(_ => IO.raiseError(BadRequestE("Invalid keywords.")), IO.pure)
        articles      <- articleService.fetchArticlesPaginated(page, ArticleFilter(NonEmptyList.fromList(validKeyWords)))
        response      <- Ok(articles)
      } yield response).recoverWith(defaultExceptionHandler)

    case GET -> Root / IntVar(articleId) =>
      (for {
        articleOpt <- articleService.getArticleDetails(ArticleId(articleId))
        article    <- articleOpt.fold(IO.raiseError[ArticleDetails](NotFoundE()))(_.pure[IO])
        response   <- Ok(article)
      } yield response).recoverWith(defaultExceptionHandler)
  }
}

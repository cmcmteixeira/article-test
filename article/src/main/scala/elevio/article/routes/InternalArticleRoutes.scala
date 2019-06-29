package elevio.article.routes

import cats.effect.IO
import elevio.article.model.{Article, ArticleId, ItemsPerPage, Page}
import elevio.article.services.ArticleService
import org.http4s.HttpRoutes
import org.http4s.dsl.impl.IntVar
import org.http4s.dsl.io._

class InternalArticleRoutes(articleService: ArticleService) {

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root :? PageParamMatcher(pageOpt) +& ItemsPerPageParamMatcher(ippOpt) =>
      (for {
        page         <- pageOpt.fold(IO.raiseError[Page](BadRequestE("Please provide a page.")))(IO.pure[Page])
        itemsPerPage <- ippOpt.fold(IO.raiseError[ItemsPerPage](BadRequestE("Please provide a itemsPerPage.")))(IO.pure[ItemsPerPage])
        page         <- articleService.getInternalArticles(page, itemsPerPage)
        response     <- Ok(page)
      } yield response).handleErrorWith(defaultExceptionHandler)
    case GET -> Root / IntVar(articleId) =>
      (for {
        articleOpt <- articleService.getInternalArticle(ArticleId(articleId))
        article    <- articleOpt.fold(IO.raiseError[Article](NotFoundE()))(IO.pure[Article])
        response   <- Ok(article)
      } yield response).handleErrorWith(defaultExceptionHandler)
  }
}

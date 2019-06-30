package elevio.service.handlers

import cats.effect.IO
import com.itv.bucky.consume._
import com.itv.bucky.RequeueHandler
import elevio.common.model.ArticleId
import elevio.service.services.ArticleService
import cats.implicits._
class ArticleUpdateHandler(articleService: ArticleService) extends RequeueHandler[IO, ArticleId] {
  override def apply(article: ArticleId): IO[RequeueConsumeAction] =
    articleService.updateInternalArticleDetails(article) *> Ack.pure[IO]
}

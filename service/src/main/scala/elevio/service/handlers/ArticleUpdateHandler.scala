package elevio.service.handlers

import cats.effect.IO
import com.itv.bucky.consume._
import elevio.common.model.{ArticleUpdate}
import elevio.service.services.ArticleService
import com.itv.bucky.RequeueHandler
import com.typesafe.scalalogging.StrictLogging
class ArticleUpdateHandler(articleService: ArticleService) extends RequeueHandler[IO, ArticleUpdate] with StrictLogging {
  override def apply(article: ArticleUpdate): IO[RequeueConsumeAction] =
    for {
      _ <- IO(logger.info(s"Processing article update for ${article.id}"))
      _ <- articleService.updateInternalArticleDetails(article.id)
    } yield Ack
}

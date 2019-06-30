package elevio.service.handlers

import cats.effect.IO
import com.itv.bucky.consume.Ack
import elevio.common.model.ArticleId
import elevio.service.services.ArticleService
import elevio.service.unit.DefaultSpec
import org.mockito.Mockito._
class ArticleUpdateHandlerSpec extends DefaultSpec {
  class Fixture {
    val articleId: ArticleId           = ArticleId(1L)
    val articleService: ArticleService = mock[ArticleService]
    val handler: ArticleUpdateHandler  = new ArticleUpdateHandler(articleService)
  }
  "The article update handler" should "update an article" in withIO {
    val f = new Fixture
    when(f.articleService.updateInternalArticleDetails(f.articleId)).thenReturn(IO.unit)
    for {
      result <- f.handler(f.articleId)
    } yield {
      verify(f.articleService).updateInternalArticleDetails(f.articleId)
      result should ===(Ack)
    }

  }
}

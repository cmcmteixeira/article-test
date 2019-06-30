package elevio.service.routes

import cats.effect.IO
import cats.implicits._
import elevio.common.model
import elevio.common.model._
import elevio.service.services.ArticleService
import elevio.service.services.ArticleService.ArticleFilter
import elevio.service.unit.DefaultSpec
import org.http4s.Uri
import org.http4s.implicits._
import org.http4s.client.Client
import org.mockito.Mockito._

class ArticleRoutesSpec extends DefaultSpec {
  class Fixture {
    val page: Page                             = Page(1L)
    val articleId: ArticleId                   = ArticleId(1)
    val title                                  = Title("A title")
    val keyWord: model.ArticleKeyWord                 = ArticleKeyWord("blah")
    val author: Author                         = Author("")
    val keywords                               = List(keyWord)
    val article: Article                       = Article(articleId, title, keywords)
    val articleDetails: ArticleDetails         = ArticleDetails(articleId, title, author, List(keyWord))
    val pagList: ServicePaginatedList[Article] = ServicePaginatedList[Article](List(article), page, PageSize(1L), PageCount(1L), EntriesCount(1L))
    val articleS: ArticleService               = mock[ArticleService]
    val articleRoutes: ArticleRoutes           = new ArticleRoutes(articleS)
    val client: Client[IO]                     = Client.fromHttpApp(articleRoutes.routes.orNotFound)
  }

  "a call to root /" should "return a response if successful" in withIO {
    val f = new Fixture
    when(f.articleS.fetchArticlesPaginated(f.page, ArticleFilter(None))).thenReturn(f.pagList.pure[IO])
    for {
      response <- f.client.expect[ServicePaginatedList[Article]](Uri.unsafeFromString("/").withQueryParam("page", 1L))
    } yield {
      response should ===(f.pagList)
    }
  }

  "a call to root /" should "return a BadRequest if page is not specified" in withIO {
    val f       = new Fixture
    val page    = Page(1L)
    val pagList = ServicePaginatedList[Article](List.empty, page, PageSize(1L), PageCount(1L), EntriesCount(1L))
    when(f.articleS.fetchArticlesPaginated(page, ArticleFilter(None))).thenReturn(pagList.pure[IO])
    for {
      statusCode <- f.client.get(Uri.unsafeFromString("/"))(_.status.pure[IO])
    } yield {
      statusCode.code should ===(400)
    }
  }

  "a call to /:id" should "return a response if successful" in withIO {
    val f = new Fixture
    when(f.articleS.getArticleDetails(f.article.id)).thenReturn(f.articleDetails.some.pure[IO])
    for {
      response <- f.client.expect[Article](Uri.unsafeFromString(s"/${f.article.id.value}"))
    } yield {
      response should ===(f.article)
    }
  }

  it should "return Not found if the article doesn't exist " in withIO {
    val f = new Fixture
    when(f.articleS.getArticleDetails(f.article.id)).thenReturn(IO(None))
    for {
      response <- f.client.get(Uri.unsafeFromString(s"/${f.article.id.value}"))(_.status.pure[IO])
    } yield {
      response.code should ===(404)
    }
  }

}

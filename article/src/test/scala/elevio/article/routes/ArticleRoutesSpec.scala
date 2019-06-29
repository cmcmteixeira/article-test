package elevio.article.routes

import cats.effect.IO
import elevio.article.model.{Article, ArticleId, EntriesCount, Page, PageCount, PageSize, ServicePaginatedList}
import elevio.article.services.ArticleService
import elevio.article.services.ArticleService.ArticleFilter
import elevio.article.unit.DefaultSpec
import org.http4s.client.Client
import org.http4s.implicits._
import org.mockito.Mockito._
import cats.implicits._
import org.http4s.Uri

class ArticleRoutesSpec extends DefaultSpec {
  class Fixture {
    val articleS: ArticleService     = mock[ArticleService]
    val articleRoutes: ArticleRoutes = new ArticleRoutes(articleS)
    val client: Client[IO]           = Client.fromHttpApp(articleRoutes.routes.orNotFound)
  }

  "a call to root /" should "return a response if successful" in withIO {
    val f       = new Fixture
    val page    = Page(1L)
    val pagList = ServicePaginatedList[Article](List.empty, page, PageSize(1L), PageCount(1L), EntriesCount(1L))
    when(f.articleS.fetchPaginatedArticleList(page, ArticleFilter(None))).thenReturn(pagList.pure[IO])
    for {
      response <- f.client.expect[ServicePaginatedList[Article]](Uri.unsafeFromString("/").withQueryParam("page", 1L))
    } yield {
      response should ===(pagList)
    }
  }

  "a call to root /" should "return a BadRequest if page is not specified" in withIO {
    val f       = new Fixture
    val page    = Page(1L)
    val pagList = ServicePaginatedList[Article](List.empty, page, PageSize(1L), PageCount(1L), EntriesCount(1L))
    when(f.articleS.fetchPaginatedArticleList(page, ArticleFilter(None))).thenReturn(pagList.pure[IO])
    for {
      statusCode <- f.client.get(Uri.unsafeFromString("/"))(_.status.pure[IO])
    } yield {
      statusCode.code should ===(400)
    }
  }

  "a call to /:id" should "return a response if successful" in withIO {
    val f       = new Fixture
    val article = Article(ArticleId(1))
    when(f.articleS.getArticleDetails(article.id)).thenReturn(article.some.pure[IO])
    for {
      response <- f.client.expect[Article](Uri.unsafeFromString(s"/${article.id.value}"))
    } yield {
      response should ===(article)
    }
  }

  it should "return Not found if the article doesn't exist " in withIO {
    val f       = new Fixture
    val article = Article(ArticleId(1))
    when(f.articleS.getArticleDetails(article.id)).thenReturn(IO(None))
    for {
      response <- f.client.get(Uri.unsafeFromString(s"/${article.id.value}"))(_.status.pure[IO])
    } yield {
      response.code should ===(404)
    }
  }

}

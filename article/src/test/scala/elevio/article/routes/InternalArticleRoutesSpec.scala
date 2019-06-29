package elevio.article.routes

import cats.effect.IO
import cats.implicits._
import elevio.article.model._
import elevio.article.services.ArticleService
import elevio.article.unit.DefaultSpec
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.implicits._
import org.mockito.Mockito._

class InternalArticleRoutesSpec extends DefaultSpec {
  class Fixture {
    val article       = Article(ArticleId(1L))
    val page          = Page(1L)
    val itemsPerPage  = ItemsPerPage(10)
    val paginatedList = ServicePaginatedList(List(article), page, PageSize(10), PageCount(10), EntriesCount(10))

    val articleS: ArticleService             = mock[ArticleService]
    val articleRoutes: InternalArticleRoutes = new InternalArticleRoutes(articleS)
    val client: Client[IO]                   = Client.fromHttpApp(articleRoutes.routes.orNotFound)
  }
  "a call to /:id" should "return an article if successful" in withIO {
    val f = new Fixture
    when(f.articleS.getInternalArticle(f.article.id)).thenReturn(f.article.some.pure[IO])
    for {
      response <- f.client.expect[Article](Uri.unsafeFromString(s"/${f.article.id.value}"))
    } yield {
      response should ===(f.article)
    }
  }

  it should "return a 404 if no article is found " in withIO {
    val f = new Fixture
    when(f.articleS.getInternalArticle(f.article.id)).thenReturn(None.pure[IO])
    for {
      status <- f.client.get(Uri.unsafeFromString(s"/${f.article.id.value}"))(_.status.pure[IO])
    } yield {
      status.code should ===(404)
    }
  }

  "a call to /" should "return a paginated list of services" in withIO {
    val f = new Fixture
    when(f.articleS.getInternalArticles(f.page, f.itemsPerPage)).thenReturn(f.paginatedList.pure[IO])
    for {
      url    <- IO.pure(Uri.unsafeFromString(s"/").withQueryParam("page", f.page.value).withQueryParam("itemsPerPage", f.itemsPerPage.value))
      result <- f.client.expect[ServicePaginatedList[Article]](url)
    } yield {
      result should ===(f.paginatedList)
    }
  }
  it should "return a 400 Bad request if itemsPerPage is not provided" in withIO {
    val f = new Fixture

    for {
      url    <- IO.pure(Uri.unsafeFromString(s"/").withQueryParam("page", f.page.value))
      result <- f.client.get(url)(_.status.pure[IO])
    } yield {
      result.code should ===(400)
    }
  }

  it should "return a 400 Bad request if page is not provided" in withIO {
    val f            = new Fixture
    val itemsPerPage = ItemsPerPage(10)

    for {
      url    <- IO.pure(Uri.unsafeFromString(s"/").withQueryParam("itemsPerPage", itemsPerPage.value))
      result <- f.client.get(url)(_.status.pure[IO])
    } yield {
      result.code should ===(400)
    }
  }

}

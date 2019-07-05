package elevio.updater.httpclients

import cats.effect.IO
import elevio.common.model._
import elevio.updater.unit.DefaultSpec
import elevio.updater.httpclients.InternalArticlesClient.InternalArticleClientConfig
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{HttpRoutes, Uri}

class InternalArticlesClientSpec extends DefaultSpec {
  class Fixture {
    object PageParamMatcher extends QueryParamDecoderMatcher[Long]("page")
    object ArticlesPerPage  extends QueryParamDecoderMatcher[Int]("itemsPerPage")
    val articleId: ArticleId                         = ArticleId(1L)
    val title: Title                                 = Title("a titel")
    val author                                       = Author("author")
    val keyword                                      = ArticleKeyWord("blah")
    val keywords                                     = List(keyword)
    val article: Article                             = Article(articleId, title, keywords)
    val page: Page                                   = Page(10)
    val pageSize: PageSize                           = PageSize(100L)
    val totalPages: PageCount                        = PageCount(10L)
    val totalEntries: EntriesCount                   = EntriesCount(100L)
    val paginatedList: ServicePaginatedList[Article] = ServicePaginatedList[Article](List(article), page, pageSize, totalPages, totalEntries)
    val config                                       = InternalArticleClientConfig(baseUrl = Uri.unsafeFromString("/"))
    val articlesPerPage                              = ItemsPerPage(10)

    val httpClient: Client[IO] = Client.fromHttpApp(
      HttpRoutes
        .of[IO] {
          case GET -> Root / "internal" / "articles" :? PageParamMatcher(p) +& ArticlesPerPage(app) =>
            if (Page(p) == page && ItemsPerPage(app) == articlesPerPage) {
              Ok(paginatedList)
            } else {
              InternalServerError("Page is not supported")
            }
          case GET -> Root / "internal" / "articles" / LongVar(id) =>
            if (id == articleId.value) {
              Ok(article)
            } else {
              NotFound("Not Found")
            }
        }
        .orNotFound)
    val service = InternalArticlesClient(httpClient, config)
  }

  "fetchInternalArticle()" should "fetch articles by id" in withIO {
    val f = new Fixture
    for {
      result <- f.service.fetchInternalArticle(f.articleId)
    } yield {
      result should ===(Some(f.article))
    }
  }
  it should "return None if the article is not found" in withIO {
    val f = new Fixture
    for {
      result <- f.service.fetchInternalArticle(ArticleId(f.article.id.value + 10000))
    } yield {
      result should ===(None)
    }
  }

  "fetchInternalArticles()" should "fetch a paginated list of results" in withIO {
    val f = new Fixture
    for {
      result <- f.service.fetchInternalArticles(f.page, f.articlesPerPage)
    } yield {
      result shouldBe f.paginatedList
    }
  }

}

package elevio.article

import cats.effect.IO
import elevio.article.httpclient.ArticleClient
import elevio.article.model.{ApiKey, Article, ArticleId, ElevioPaginatedList, EntriesCount, JWT, Page, PageCount, PageSize}
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.dsl.io._
import org.http4s.util.CaseInsensitiveString
import org.http4s.{HttpRoutes, Request, Uri}
import org.scalatest.{FlatSpec, Matchers}
import unit._
import cats.implicits._
import elevio.article.httpclient.ArticleClient.ArticleClientConfig

class ArticleClientSpec extends DefaultSpec {
  class Fixture {
    object PageParamMatcher extends QueryParamDecoderMatcher[Long]("page")

    val articleId: ArticleId                        = ArticleId(1L)
    val article: Article                            = Article(articleId)
    val page: Page                                  = Page(10)
    val pageSize: PageSize                          = PageSize(100L)
    val totalPages: PageCount                       = PageCount(10L)
    val totalEntries: EntriesCount                  = EntriesCount(100L)
    val token: JWT                                  = JWT("A Token")
    val apiKey: ApiKey                              = ApiKey("anApiKey")
    val paginatedList: ElevioPaginatedList[Article] = ElevioPaginatedList[Article](List(article), page, pageSize, totalPages, totalEntries)
    val config                                      = ArticleClientConfig(basePath = Uri.unsafeFromString("/"), "v1", token, apiKey)

    private def validAuthHeaders(r: Request[IO]): Boolean =
      (r.headers.get(CaseInsensitiveString("x-api-key")), r.headers.get(CaseInsensitiveString("Authorization")))
        .mapN((keyHeader, authHeader) => keyHeader.value == apiKey.value && authHeader.value == token.value)
        .getOrElse(false)

    val httpClient: Client[IO] = Client.fromHttpApp(
      HttpRoutes
        .of[IO] {
          case r @ GET -> Root / version / "articles" :? PageParamMatcher(p) =>
            if (Page(p) == page && validAuthHeaders(r) && version == config.version) {
              Ok(paginatedList)
            } else if (!validAuthHeaders(r)) {
              InternalServerError("Should be Unauthorized but the compiler meh !")
            } else {
              InternalServerError("Page is not supported")
            }
          case r @ GET -> Root / version / "articles" / LongVar(id) =>
            if (id == articleId.value && validAuthHeaders(r) && version == config.version) {
              Ok(article)
            } else if (!validAuthHeaders(r)) {
              InternalServerError("Should be Unauthorized but the compiler meh !")
            } else {
              NotFound("Not Found")
            }
        }
        .orNotFound)
    val service             = ArticleClient(httpClient, config)
    val unauthorizedService = ArticleClient(httpClient, config.copy(apiKey = ApiKey("something else")))
  }

  "fetchArticle()" should "fetch articles by client id" in withIO {
    val f = new Fixture
    for {
      result <- f.service.fetchArticle(f.articleId)
    } yield {
      result shouldBe Some(f.article)
    }
  }
  it should "return None if the article is not found" in withIO {
    val f = new Fixture
    for {
      result <- f.service.fetchArticle(ArticleId(f.article.id.value + 10000))
    } yield {
      result shouldBe None
    }
  }
  it should "return a failed IO if an error occurs" in withIO {
    val f = new Fixture
    for {
      result <- f.unauthorizedService.fetchArticle(f.article.id).attempt
    } yield {
      result shouldBe 'Left
    }
  }

  "fetchPaginatedArticleList()" should "fetch a paginated list of results" in withIO {
    val f = new Fixture
    for {
      result <- f.service.fetchPaginatedArticleList(f.page)
    } yield {
      result shouldBe f.paginatedList
    }
  }
  it should "return a failed IO if an unexpected result is returned" in withIO {
    val f = new Fixture
    for {
      result <- f.unauthorizedService.fetchPaginatedArticleList(f.page).attempt
    } yield {
      result shouldBe 'Left
    }
  }
}
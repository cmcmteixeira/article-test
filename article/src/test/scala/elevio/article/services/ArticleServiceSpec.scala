package elevio.article.services

import cats.data.NonEmptyList
import cats.effect.IO
import elevio.article.httpclient.ArticleClient
import elevio.article.model.{
  Article,
  ArticleId,
  ElevioPaginatedList,
  EntriesCount,
  ItemsPerPage,
  KeyWord,
  Page,
  PageCount,
  PageSize,
  ServicePaginatedList
}
import elevio.article.repository.ArticleRepository
import elevio.article.services.ArticleService.{ArticleFilter, ArticleServiceConfig}
import elevio.article.unit.DefaultSpec
import org.mockito.Mockito._
import cats.implicits._

class ArticleServiceSpec extends DefaultSpec {
  class Feature {
    val articleClient: ArticleClient         = mock[ArticleClient]
    val articleRepository: ArticleRepository = mock[ArticleRepository]
    val config: ArticleServiceConfig         = ArticleServiceConfig(ItemsPerPage(10))
    val service: ArticleService              = ArticleService(articleClient, articleRepository, config)
  }
  "getArticleDetails()" should "fetch the article" in withIO {
    val f         = new Feature
    val articleId = ArticleId(3L)
    val article   = Article(articleId)

    when(f.articleClient.fetchArticle(articleId)).thenReturn(IO(Some(article)))

    for {
      result <- f.service.getArticleDetails(articleId)
    } yield {
      result should ===(Some(article))
    }
  }

  it should "return none if no article can be found" in withIO {
    val f         = new Feature
    val articleId = ArticleId(3L)

    when(f.articleClient.fetchArticle(articleId)).thenReturn(IO(None))

    for {
      result <- f.service.getArticleDetails(articleId)
    } yield {
      result should ===(None)
    }
  }

  "fetchPaginatedArticleList()" should "fetch a paginated list of articles for a page" in withIO {
    val f             = new Feature
    val page          = Page(1L)
    val article       = Article(ArticleId(1L))
    val elements      = List(article)
    val paginatedList = ElevioPaginatedList(elements, page, PageSize(100L), PageCount(10L), EntriesCount(10L))

    when(f.articleClient.fetchPaginatedArticleList(page)).thenReturn(paginatedList.pure[IO])

    for {
      result <- f.service.fetchPaginatedArticleList(page, ArticleFilter(None))
    } yield {
      //result shouldBe ===(paginatedList.toServicePaginatedList) TODO: For some weird reason this keeps failing :/
      ServicePaginatedList.unapply(result) should ===(ServicePaginatedList.unapply(paginatedList.toServicePaginatedList))
    }
  }

  it should "fallback to using the database if a filter keyword is provided" in withIO {
    val f             = new Feature
    val page          = Page(1L)
    val filter        = ArticleFilter(Some(KeyWord("").pure[NonEmptyList]))
    val elements      = List.empty[Article]
    val paginatedList = ElevioPaginatedList(elements, page, PageSize(100L), PageCount(10L), EntriesCount(10L))
    val serivePagList = paginatedList.toServicePaginatedList

    when(f.articleRepository.findArticlesFor(page, f.config.itemsPerPage)(filter)).thenReturn(serivePagList.pure[IO])

    for {
      result <- f.service.fetchPaginatedArticleList(page, filter)
    } yield {
      result should ===(serivePagList)
    }
  }

  "updateInternalArticleDetails()" should "update the internal details of an article" in withIO {
    val f           = new Feature
    val article     = Article(ArticleId(1))
    val newKeyWords = List(KeyWord("blah"))
    when(f.articleClient.fetchArticle(article.id)).thenReturn(article.some.pure[IO])
    when(f.articleRepository.upsertArticle(article, newKeyWords)).thenReturn(IO.unit)
    for {
      _ <- f.service.updateInternalArticleDetails(article.id)
    } yield ()
  }

  it should "remove an article if it's not found" in withIO {
    val f       = new Feature
    val article = Article(ArticleId(1))

    when(f.articleClient.fetchArticle(article.id)).thenReturn(None.pure[IO])
    when(f.articleRepository.deleteArticle(article.id)).thenReturn(IO.unit)

    for {
      _ <- f.service.updateInternalArticleDetails(article.id)
    } yield ()
  }

  it should "fail if an error is raised" in withIO {
    val f       = new Feature
    val article = Article(ArticleId(1))

    when(f.articleClient.fetchArticle(article.id)).thenReturn(IO.raiseError(new Throwable("Some error")))

    for {
      result <- f.service.updateInternalArticleDetails(article.id).attempt
    } yield {
      result shouldBe 'Left
    }
  }

  "getInternalArticleDetails()" should "fetch internal article" in withIO {
    val f       = new Feature
    val article = Article(ArticleId(1))

    when(f.articleRepository.findArticleFor(article.id)).thenReturn(article.some.pure[IO])

    for {
      result <- f.service.getInternalArticle(article.id)
    } yield {
      result should ===(Some(article))
    }
  }

  it should "return none if not found" in withIO {
    val f       = new Feature
    val article = Article(ArticleId(1))

    when(f.articleRepository.findArticleFor(article.id)).thenReturn(None.pure[IO])

    for {
      result <- f.service.getInternalArticle(article.id)
    } yield {
      result should ===(None)
    }
  }

  "getInternalArticles()" should "fetch a paginated list of articles" in withIO {
    val f            = new Feature
    val article      = Article(ArticleId(1))
    val page         = ServicePaginatedList(List(article), Page(1L), PageSize(1000), PageCount(100), EntriesCount(100))
    val itemsPerPage = ItemsPerPage(10)
    val filter       = ArticleFilter(None)
    when(f.articleRepository.findArticlesFor(page.page, itemsPerPage)(filter)).thenReturn(page.pure[IO])

    for {
      result <- f.service.getInternalArticles(page.page, itemsPerPage)
    } yield {
      result should ===(page)
    }
  }

}

package elevio.service.services

import java.time.ZonedDateTime

import cats.data.NonEmptyList
import cats.effect.IO
import cats.implicits._
import elevio.common.httpclient.ElevioArticleClient
import elevio.common.model._
import elevio.service.repository.ArticleRepository
import elevio.service.services.ArticleService.{ArticleFilter, ArticleServiceConfig}
import elevio.service.unit.DefaultSpec
import org.mockito.Mockito._

class ArticleServiceSpec extends DefaultSpec {
  class Feature {
    val articleId: ArticleId           = ArticleId(3L)
    val keyword: ArticleKeyWord        = ArticleKeyWord("something")
    val keywords: List[ArticleKeyWord] = List(keyword)
    val title: Title                   = Title("title")
    val author: Author                 = Author("author")
    val article                        = Article(articleId, title, keywords)
    val articleDetails: ArticleDetails =
      ArticleDetails(articleId, title, author, keywords, ZonedDateTime.now(), ZonedDateTime.now(), Some(Author("someone")), Version("version"))
    val page: Page                                  = Page(1L)
    val elements: List[Article]                     = List(article)
    val paginatedList: ElevioPaginatedList[Article] = ElevioPaginatedList(elements, page, PageSize(100L), PageCount(10L), EntriesCount(10L))
    val itemsPerPage: ItemsPerPage                  = ItemsPerPage(10)
    val articleClient: ElevioArticleClient          = mock[ElevioArticleClient]
    val articleRepository: ArticleRepository        = mock[ArticleRepository]
    val config: ArticleServiceConfig                = ArticleServiceConfig(ItemsPerPage(10))
    val service: ArticleService                     = ArticleService(articleClient, articleRepository, config)
  }
  "getArticleDetails()" should "fetch the article" in withIO {
    val f = new Feature
    when(f.articleClient.fetchArticle(f.articleId)).thenReturn(IO(Some(f.articleDetails)))
    for {
      result <- f.service.getArticleDetails(f.articleId)
    } yield {
      result should ===(Some(f.articleDetails))
    }
  }

  it should "return none if no article can be found" in withIO {
    val f = new Feature
    when(f.articleClient.fetchArticle(f.articleId)).thenReturn(IO(None))
    for {
      result <- f.service.getArticleDetails(f.articleId)
    } yield {
      result should ===(None)
    }
  }

  "fetchPaginatedArticleList()" should "fetch a paginated list of articles for a page" in withIO {
    val f = new Feature

    when(f.articleClient.fetchPaginatedArticleList(f.page)).thenReturn(f.paginatedList.pure[IO])

    for {
      result <- f.service.fetchArticlesPaginated(f.page, ArticleFilter(None))
    } yield {
      //result shouldBe ===(paginatedList.toServicePaginatedList) TODO: For some weird reason this keeps failing :/
      ServicePaginatedList.unapply(result) should ===(ServicePaginatedList.unapply(f.paginatedList.toServicePaginatedList))
    }
  }

  it should "fallback to using the database if a filter keyword is provided" in withIO {
    val f             = new Feature
    val filter        = ArticleFilter(Some(ArticleKeyWord("keyword").pure[NonEmptyList]))
    val serivePagList = f.paginatedList.toServicePaginatedList
    when(f.articleRepository.findArticlesFor(f.page, f.config.itemsPerPage)(filter)).thenReturn(serivePagList.pure[IO])

    for {
      result <- f.service.fetchArticlesPaginated(f.page, filter)
    } yield {
      result should ===(serivePagList)
    }
  }

  "updateInternalArticleDetails()" should "update the internal details of an article" in withIO {
    val f = new Feature
    when(f.articleClient.fetchArticle(f.article.id)).thenReturn(f.articleDetails.some.pure[IO])
    when(f.articleRepository.upsertArticle(f.article)).thenReturn(IO.unit)
    for {
      _ <- f.service.updateInternalArticleDetails(f.article.id)
    } yield ()
  }

  it should "remove an article if it's not found" in withIO {
    val f = new Feature

    when(f.articleClient.fetchArticle(f.article.id)).thenReturn(None.pure[IO])
    when(f.articleRepository.deleteArticle(f.article.id)).thenReturn(IO.unit)

    for {
      _ <- f.service.updateInternalArticleDetails(f.article.id)
    } yield ()
  }

  it should "fail if an error is raised" in withIO {
    val f = new Feature
    when(f.articleClient.fetchArticle(f.article.id)).thenReturn(IO.raiseError(new Throwable("Some error")))

    for {
      result <- f.service.updateInternalArticleDetails(f.article.id).attempt
    } yield {
      result shouldBe 'Left
    }
  }

  "getInternalArticleDetails()" should "fetch internal article" in withIO {
    val f = new Feature

    when(f.articleRepository.findArticleFor(f.article.id)).thenReturn(f.article.some.pure[IO])

    for {
      result <- f.service.getInternalArticle(f.article.id)
    } yield {
      result should ===(Some(f.article))
    }
  }

  it should "return none if not found" in withIO {
    val f = new Feature
    when(f.articleRepository.findArticleFor(f.article.id)).thenReturn(None.pure[IO])

    for {
      result <- f.service.getInternalArticle(f.article.id)
    } yield {
      result should ===(None)
    }
  }

  "getInternalArticles()" should "fetch a paginated list of articles" in withIO {
    val f             = new Feature
    val filter        = ArticleFilter(None)
    val serivePagList = f.paginatedList.toServicePaginatedList
    when(f.articleRepository.findArticlesFor(f.page, f.itemsPerPage)(filter)).thenReturn(serivePagList.pure[IO])

    for {
      result <- f.service.getInternalArticles(f.page, f.itemsPerPage)
    } yield {
      result should ===(f.paginatedList.toServicePaginatedList)
    }
  }

}

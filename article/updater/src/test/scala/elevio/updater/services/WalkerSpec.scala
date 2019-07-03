package elevio.updater.services

import java.time.ZonedDateTime

import cats.effect.{ContextShift, IO, Timer}
import com.itv.bucky.Publisher
import elevio.common.httpclient.ElevioArticleClient
import elevio.common.model.{
  Article,
  ArticleDetails,
  ArticleId,
  ArticleKeyWord,
  ArticleUpdate,
  Author,
  ElevioPaginatedList,
  EntriesCount,
  ItemsPerPage,
  Page,
  PageCount,
  PageSize,
  Title,
  Version
}
import elevio.updater.httpclients.InternalArticlesClient
import elevio.updater.services.ElevioWalker.ElevioWalkerConfig
import elevio.updater.unit.DefaultSpec
import org.mockito.Mockito._
import cats.implicits._

import scala.concurrent.duration._
import elevio.updater.services.InternalWalker.InternalWalkerConfig
class WalkerSpec extends DefaultSpec {
  class Fixture(cs: ContextShift[IO], timer: Timer[IO]) {

    val elevioConfig: ElevioWalkerConfig        = ElevioWalkerConfig(1, 1.seconds)
    val internalConfig: InternalWalkerConfig    = InternalWalkerConfig(1, ItemsPerPage(2), 1.seconds)
    val internalClient: InternalArticlesClient  = mock[InternalArticlesClient]
    val elevioClient: ElevioArticleClient       = mock[ElevioArticleClient]
    val publisher: Publisher[IO, ArticleUpdate] = mock[Publisher[IO, ArticleUpdate]]
    val baseArticleDetails = ArticleDetails(ArticleId(1L),
                                            Title(""),
                                            Author(""),
                                            List(ArticleKeyWord("k1")),
                                            ZonedDateTime.now(),
                                            ZonedDateTime.now(),
                                            None,
                                            Version("some"))

    val page1ArticleDetails = List(
      baseArticleDetails.copy(id = ArticleId(1L), title = Title("a new title")),
      baseArticleDetails.copy(id = ArticleId(2L), keywords = List.empty)
    )
    val page2ArticleDetails = List(
      baseArticleDetails.copy(id = ArticleId(3L), title = Title("a new title"))
    )

    val page1Articles: List[Article] = page1ArticleDetails.map(_.toArticle)
    val page2Articles: List[Article] = page2ArticleDetails.map(_.toArticle)

    val article1Page1Details :: page1Article2Details :: Nil = page1ArticleDetails
    val article3Page2Details :: Nil                         = page2ArticleDetails
    val article1Page1 :: article2Page1 :: _                 = page1Articles
    val article3Page2 :: _                                  = page2Articles

    val page1 = ElevioPaginatedList(page1Articles, Page(1L), PageSize(page1Articles.size.toLong), PageCount(2L), EntriesCount(3L))
    val page2 = ElevioPaginatedList(page2Articles, Page(2L), PageSize(page1Articles.size.toLong), PageCount(2L), EntriesCount(3L))

    val elevioWalker   = ElevioWalker(elevioConfig, elevioClient, internalClient, publisher)(cs, timer)
    val internalWalker = InternalWalker(internalConfig, elevioClient, internalClient, publisher)(cs, timer)
  }
  "The Elevio Walker" should "iterate over elevio " in withResources({ (cs, timer) =>
    val f = new Fixture(cs, timer)
    when(f.elevioClient.fetchPaginatedArticleList(Page(1))).thenReturn(f.page1.pure[IO])
    when(f.elevioClient.fetchPaginatedArticleList(Page(2))).thenReturn(f.page2.pure[IO])

    when(f.internalClient.fetchInternalArticle(f.article1Page1.id)).thenReturn(f.article1Page1.some.pure[IO])
    when(f.internalClient.fetchInternalArticle(f.article2Page1.id)).thenReturn(None.pure[IO])
    when(f.internalClient.fetchInternalArticle(f.article3Page2.id)).thenReturn(f.article3Page2.copy(title = Title("Something else")).some.pure[IO])

    when(f.publisher.apply(ArticleUpdate(f.article2Page1.id))).thenReturn(IO.unit)
    when(f.publisher.apply(ArticleUpdate(f.article3Page2.id))).thenReturn(IO.unit)

    for {
      _ <- f.elevioWalker.run
    } yield {
      verify(f.publisher).apply(ArticleUpdate(f.article2Page1.id))
      verify(f.publisher).apply(ArticleUpdate(f.article3Page2.id))
    }
  })

  "The Article Walker" should "iterate over the article service" in withResources({ (cs, timer) =>
    val f = new Fixture(cs, timer)
    when(f.internalClient.fetchInternalArticles(Page(1), f.internalConfig.itemsPerPage)).thenReturn(f.page1.toServicePaginatedList.pure[IO])
    when(f.internalClient.fetchInternalArticles(Page(2), f.internalConfig.itemsPerPage)).thenReturn(f.page2.toServicePaginatedList.pure[IO])

    when(f.elevioClient.fetchArticle(f.article1Page1.id)).thenReturn(f.article1Page1Details.some.pure[IO])
    when(f.elevioClient.fetchArticle(f.article2Page1.id)).thenReturn(None.pure[IO])
    when(f.elevioClient.fetchArticle(f.article3Page2.id)).thenReturn(f.article3Page2Details.copy(title = Title("Something else")).some.pure[IO])

    when(f.publisher.apply(ArticleUpdate(f.article2Page1.id))).thenReturn(IO.unit)
    when(f.publisher.apply(ArticleUpdate(f.article3Page2.id))).thenReturn(IO.unit)
    for {
      _ <- f.internalWalker.run
    } yield {
      verify(f.publisher).apply(ArticleUpdate(f.article2Page1.id))
      verify(f.publisher).apply(ArticleUpdate(f.article3Page2.id))
    }
  })
}

package elevio.service.repository

import java.util.concurrent.Executors

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import doobie.h2.H2Transactor
import doobie.util.transactor.Transactor
import elevio.service.app.{ThreadPoolConfig, dbConnectionThreadPool, dbTransactionThreadPool}
import org.flywaydb.core.Flyway
import cats.implicits._
import elevio.common.model.{Article, ArticleId, ArticleKeyWord, ItemsPerPage, Page, PageCount, Title}
import elevio.service.services.ArticleService.ArticleFilter
import elevio.service.unit.DefaultSpec

import scala.concurrent.ExecutionContext

class ArticleRepositorySpec extends DefaultSpec {
  class Fixture(tx: Transactor[IO]) {
    val keywords: List[ArticleKeyWord] = List(ArticleKeyWord("key 1"), ArticleKeyWord("key 2"))
    val article: Article               = Article(ArticleId(1), Title("title"), keywords)
    val repository: ArticleRepository  = ArticleRepository(tx)
  }

  "upsertArticle()" should "insert an article with no keywords in the database" in withDatabase { tx =>
    val f = new Fixture(tx)
    for {
      _      <- f.repository.upsertArticle(f.article.copy(keywords = List.empty))
      result <- f.repository.findArticleFor(f.article.id)
    } yield {
      result should ===(Some(f.article.copy(keywords = List.empty)))
    }
  }

  it should "insert an article with keywords in the database" in withDatabase { tx =>
    val f = new Fixture(tx)
    for {
      _      <- f.repository.upsertArticle(f.article)
      result <- f.repository.findArticleFor(f.article.id)
    } yield {
      result should ===(Some(f.article))
    }
  }

  "deleteArticle()" should "delete an article from the database" in withDatabase { tx =>
    val f = new Fixture(tx)
    for {
      _      <- f.repository.upsertArticle(f.article)
      _      <- f.repository.deleteArticle(f.article.id)
      result <- f.repository.findArticleFor(f.article.id)
    } yield {
      result should ===(None)
    }
  }

  "findArticlesFor(page,...)" should "return a paginated list of results" in withDatabase { tx =>
    val f        = new Fixture(tx)
    val articles = (1 to 100).map(id => f.article.copy(id = ArticleId(id))).toList
    for {
      _     <- articles.traverse(f.repository.upsertArticle)
      page1 <- f.repository.findArticlesFor(Page(1), ItemsPerPage(50))(ArticleFilter())
      page2 <- f.repository.findArticlesFor(Page(2), ItemsPerPage(50))(ArticleFilter())
    } yield {
      page1.elements should have size 50
      page2.elements should have size 50
      page1.totalPages should ===(PageCount(2))
      page2.totalPages should ===(PageCount(2))
      page1.page should ===(Page(1))
      page2.page should ===(Page(2))
      (page1.elements ++ page2.elements).sortBy(_.id.value) should ===(articles)
    }
  }

  it should "allow filtering by keyword" in withDatabase { tx =>
    val f          = new Fixture(tx)
    val newKeyword = ArticleKeyWord("A new keyword")
    val articles   = (1 to 100).map(id => f.article.copy(id = ArticleId(id))).toList
    val articles2  = (101 to 200).map(id => f.article.copy(id = ArticleId(id), keywords = List(newKeyword))).toList
    for {
      _ <- (articles ++ articles2).traverse(f.repository.upsertArticle)
      page1 <- f.repository.findArticlesFor(Page(1), ItemsPerPage(300))(
        ArticleFilter(keywords = Some(NonEmptyList.of(newKeyword, f.article.keywords: _*))))
      page2 <- f.repository.findArticlesFor(Page(1), ItemsPerPage(300))(ArticleFilter(keywords = Some(NonEmptyList.of(newKeyword))))
    } yield {
      page1.elements should have size 200
      page2.elements should have size 100
      page1.totalPages should ===(PageCount(1))
      page2.totalPages should ===(PageCount(1))
      page1.page should ===(Page(1))
      page2.page should ===(Page(1))
      page1.elements.sortBy(_.id.value) should ===(articles ++ articles2)
      page2.elements.sortBy(_.id.value) should ===(articles2)
    }
  }

  def withDatabase[A](test: Transactor[IO] => IO[A]): A =
    withIO((for {
      executor <- Resource.make(IO(Executors.newFixedThreadPool(5)))(tp => IO(tp.shutdown()))
      ec = ExecutionContext.fromExecutor(executor)
      dbConnectionEc <- dbConnectionThreadPool(ThreadPoolConfig(2)).map(ExecutionContext.fromExecutor)
      dbTransactEc   <- dbTransactionThreadPool().map(ExecutionContext.fromExecutor)
      tx <- H2Transactor
        .newH2Transactor[IO]("jdbc:h2:mem:test;;MODE=PostgreSQL", "", "", dbConnectionEc, dbTransactEc)(implicitly, IO.contextShift(ec))

    } yield tx).use(tx => {
      IO {
        val flyway = new Flyway
        flyway.setDataSource(tx.kernel)
        flyway.migrate()
        ()
      } *> test(tx)
    }))

}

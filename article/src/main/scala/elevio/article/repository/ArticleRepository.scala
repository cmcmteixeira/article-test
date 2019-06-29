package elevio.article.repository

import cats.effect.IO

import doobie.util.transactor.Transactor
import doobie.implicits._
import elevio.article.model.{Article, ArticleId, EntriesCount, ItemsPerPage, KeyWord, Page, PageCount, PageSize, ServicePaginatedList}
import elevio.article.services.ArticleService.ArticleFilter

trait ArticleRepository {
  def upsertArticle(article: Article, keywords: List[KeyWord]): IO[Unit]
  def deleteArticle(article: ArticleId): IO[Unit]
  def findArticlesFor(page: Page, itemsPerPage: ItemsPerPage)(filter: ArticleFilter): IO[ServicePaginatedList[Article]]
  def findArticleFor(articleId: ArticleId): IO[Option[Article]]
}

object ArticleRepository {
  def apply(tx: Transactor[IO]): ArticleRepository = new ArticleRepository {
    override def upsertArticle(article: Article, keywords: List[KeyWord]): IO[Unit] =
      (for {
        _ <- Queries.removeKeyWordsFor(article.id)
        _ <- Queries.removeArticle(article.id)
        _ <- Queries.insertArticle(article)
        _ <- Queries.insertKeyWordFor(article.id, keywords)
      } yield ()).transact(tx)

    override def findArticlesFor(page: Page, itemsPerPage: ItemsPerPage)(filter: ArticleFilter): IO[ServicePaginatedList[Article]] =
      (for {
        articles   <- Queries.articlesFor(page, itemsPerPage)(filter.keywords)
        totalCount <- Queries.articleCountFor(filter.keywords)
        pageCount  <- AsyncConnectionIO.delay(Math.ceil(totalCount / itemsPerPage.value).toInt)
      } yield
        ServicePaginatedList[Article](
          articles,
          page,
          PageSize(articles.size),
          PageCount(pageCount),
          EntriesCount(totalCount)
        )).transact(tx)

    override def deleteArticle(article: ArticleId): IO[Unit] =
      (for {
        _ <- Queries.removeKeyWordsFor(article)
        _ <- Queries.removeArticle(article)
      } yield ()).transact(tx)

    override def findArticleFor(articleId: ArticleId): IO[Option[Article]] =
      Queries.findArticleBy(articleId).transact(tx)

  }
}

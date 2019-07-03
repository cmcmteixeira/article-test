package elevio.service.repository

import cats.data.NonEmptyList
import cats.effect.IO
import doobie.util.transactor.Transactor
import doobie.implicits._
import elevio.common.model.{Article, ArticleId, ArticleKeyWord, EntriesCount, ItemsPerPage, Page, PageCount, PageSize, ServicePaginatedList, Title}
import elevio.service.services.ArticleService.ArticleFilter
import cats.implicits._
trait ArticleRepository {
  def upsertArticle(article: Article, keywords: List[ArticleKeyWord]): IO[Unit]
  def deleteArticle(article: ArticleId): IO[Unit]
  def findArticlesFor(page: Page, itemsPerPage: ItemsPerPage)(filter: ArticleFilter): IO[ServicePaginatedList[Article]]
  def findArticleFor(articleId: ArticleId): IO[Option[Article]]
}

object ArticleRepository {

  private[repository] case class RepositorySimpleArticle(id: ArticleId, title: Title)
  private[repository] case class RepositoryKeywords(articleId: ArticleId, keyWord: ArticleKeyWord)

  def apply(tx: Transactor[IO]): ArticleRepository = new ArticleRepository {
    override def upsertArticle(article: Article, keywords: List[ArticleKeyWord]): IO[Unit] =
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
        keywords   <- articles.map(_.id).toNel.fold(AsyncConnectionIO.pure(List.empty[RepositoryKeywords]))(Queries.keywordsFor)
        groupedKeywords = keywords.groupBy(_.articleId)
        pageCount <- AsyncConnectionIO.delay(Math.ceil(totalCount / itemsPerPage.value).toInt)
      } yield
        ServicePaginatedList[Article](
          articles.map(a => Article(a.id, a.title, groupedKeywords.get(a.id).toList.flatten.map(_.keyWord))),
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
      (for {
        article  <- Queries.findArticleBy(articleId)
        keywords <- article.map(_.id).map(a => NonEmptyList.of(a)).fold(AsyncConnectionIO.pure(List.empty[RepositoryKeywords]))(Queries.keywordsFor)
      } yield article.map(a => Article(a.id, a.title, keywords.map(_.keyWord)))).transact(tx)

  }
}

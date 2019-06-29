package elevio.article.repository
import cats.data.NonEmptyList
import doobie.implicits._
import doobie._
import elevio.article.model.{Article, ArticleId, ItemsPerPage, KeyWord, Page}
import shapeless._
import cats.implicits._

object Queries {
  type DBKeyword = ArticleId :: KeyWord :: HNil
  def articlesFor(page: Page, itemsPerPage: ItemsPerPage)(keyWords: Option[NonEmptyList[KeyWord]]): ConnectionIO[List[Article]] =
    (fr"""
         SELECT
          articles.id
        FROM
          articles INNER JOIN
          keywords ON (keywords.article = articles.id)
       """ ++ Fragments.whereAndOpt(
      keyWords.map(Fragments.in(fr"keyword.word", _))
    ) ++ fr""""
          LIMIT ${itemsPerPage.value}
          OFFSET ${page.value * itemsPerPage.value.toLong}
        """)
      .query[Article]
      .to[List]
      .map(_.distinct)

  def articleCountFor(keyWords: Option[NonEmptyList[KeyWord]]): ConnectionIO[Long] =
    (fr"""
         SELECT
          COUNT(DISTINCT articles.id)
        FROM
          articles INNER JOIN
          keywords ON (keywords.article = articles.id)
       """ ++ Fragments.whereAndOpt(
      keyWords.map(Fragments.in(fr"keywords.word", _))
    )).query[Long].unique

  def findArticleBy(id: ArticleId): ConnectionIO[Option[Article]] =
    (fr"""
         SELECT
          articles.id
        FROM
          articles
        WHERE
          article.id = ${id.value}
       """)
      .query[Article]
      .option

  def removeArticle(articleId: ArticleId): doobie.ConnectionIO[Unit] =
    (fr""""
          DELETE FROM
            articles
        """ ++ Fragments.whereAnd(fr"articles.id = $articleId")).update.run.void

  def removeKeyWordsFor(articleId: ArticleId): doobie.ConnectionIO[Unit] =
    (fr""""
          DELETE FROM
            keywords
        """ ++ Fragments.whereAnd(fr"keywords.article = $articleId")).update.run.void

  def insertKeyWordFor(articleId: ArticleId, keyWords: List[KeyWord]): doobie.ConnectionIO[Unit] =
    Update[DBKeyword](sql"INSERT INTO keywords (article, keyword)".update.sql).updateMany(keyWords.map(k => articleId :: k :: HNil)).void

  def insertArticle(article: Article): doobie.ConnectionIO[Unit] =
    Update[Article](sql"INSERT INTO article (id, keyword)".update.sql).updateMany(List(article)).void

}

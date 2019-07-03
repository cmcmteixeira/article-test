package elevio.service.repository
import cats.data.NonEmptyList
import doobie.implicits._
import doobie._
import elevio.common.model.{Article, ArticleId, ArticleKeyWord, ItemsPerPage, Page, Title}
import shapeless._
import cats.implicits._
import elevio.service.repository.ArticleRepository.{RepositoryKeywords, RepositorySimpleArticle}

object Queries {

  type DBArticle         = ArticleId :: Title :: HNil
  type DBKeyword         = ArticleId :: ArticleKeyWord :: HNil
  type DBArticleWKeyword = ArticleId :: Title :: Option[ArticleKeyWord] :: HNil

  def articlesFor(page: Page, itemsPerPage: ItemsPerPage)(
      keyWords: Option[NonEmptyList[ArticleKeyWord]]): ConnectionIO[List[RepositorySimpleArticle]] =
    (fr"""
         SELECT
          articles.id,
          articles.title
        FROM
          articles INNER JOIN
          ( SELECT
              a.id
            FROM
              articles a INNER JOIN
              keywords k ON (a.id = k.article_id)
              """ ++ Fragments.whereAndOpt(keyWords.map(Fragments.in(fr"keywords.word", _))) ++ fr"""
          ) articles_w_keyword ON (articles_w_keyword.id = articles.id)""" ++ fr""""
          LIMIT ${itemsPerPage.value}
          OFFSET ${page.value * itemsPerPage.value.toLong}
        """)
      .query[DBArticle]
      .map { case id :: title :: HNil => RepositorySimpleArticle(id, title) }
      .to[List]

  def keywordsFor(articles: NonEmptyList[ArticleId]): ConnectionIO[List[RepositoryKeywords]] =
    (fr"""
        SELECT 
          keywords.article_id,
          keywords.word
        FROM 
          keywords 
        WHERE
     """ ++ Fragments.in(fr"keywords.article_id", articles))
      .query[DBKeyword]
      .map { case articleId :: keyword :: HNil => RepositoryKeywords(articleId, keyword) }
      .to[List]

  def articleCountFor(keyWords: Option[NonEmptyList[ArticleKeyWord]]): ConnectionIO[Long] =
    (fr"""
         SELECT
          COUNT(DISTINCT articles.id)
        FROM
          articles INNER JOIN
          keywords ON (keywords.article = articles.id)
       """ ++ Fragments.whereAndOpt(
      keyWords.map(Fragments.in(fr"keywords.word", _))
    )).query[Long].unique

  def findArticleBy(id: ArticleId): ConnectionIO[Option[RepositorySimpleArticle]] =
    fr"""
         SELECT
           articles.id,
           articles.title
        FROM
          articles 
        WHERE
          article.id = ${id.value}
       """
      .query[DBArticle]
      .map { case id :: title :: HNil => RepositorySimpleArticle(id, title) }
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

  def insertKeyWordFor(articleId: ArticleId, keyWords: List[ArticleKeyWord]): doobie.ConnectionIO[Unit] =
    Update[DBKeyword](sql"INSERT INTO keywords (article, keyword)".update.sql)
      .updateMany(keyWords.map(k => articleId :: k :: HNil))
      .map(_ => ())

  def insertArticle(article: Article): doobie.ConnectionIO[Unit] =
    Update[DBArticle](sql"INSERT INTO article (id, title)".update.sql)
      .toUpdate0(article.id :: article.title :: HNil)
      .run
      .map(_ => ())

}

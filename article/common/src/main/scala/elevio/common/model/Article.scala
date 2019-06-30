package elevio.common.model

import cats.effect.IO
import elevio.common.httpclient._
import io.circe.{Decoder, Encoder}
import org.http4s.{EntityDecoder, EntityEncoder, QueryParamDecoder, QueryParamEncoder}
import org.http4s.QueryParamEncoder.{longQueryParamEncoder, stringQueryParamEncoder}
import io.circe.generic.semiauto._

case class Title(value: String)          extends AnyVal
case class Author(value: String)         extends AnyVal
case class ArticleKeyWord(value: String) extends AnyVal
case class ArticleId(value: Long)        extends AnyVal
case class Article(id: ArticleId, title: Title, keywords: List[ArticleKeyWord])
case class ArticleUpdate(id: ArticleId)
case class ArticleDetails(id: ArticleId, title: Title, author: Author, keywords: List[ArticleKeyWord]) {
  def toArticle: Article = Article(id, title, keywords)
}

sealed trait ArticleStatus
case object Published extends ArticleStatus
case object Drag      extends ArticleStatus

object Title {
  implicit val encoder: Encoder[Title]                 = Encoder.encodeString.contramap(_.value)
  implicit val decoder: Decoder[Title]                 = Decoder.decodeString.map(Title(_))
  implicit val entityEncoder: EntityEncoder[IO, Title] = jsonEncoderOf[Title]
  implicit val entityDecoder: EntityDecoder[IO, Title] = jsonDecoderOf[Title]

}
object Author {
  implicit val encoder: Encoder[Author]                 = Encoder.encodeString.contramap(_.value)
  implicit val decoder: Decoder[Author]                 = Decoder.decodeString.map(Author(_))
  implicit val entityEncoder: EntityEncoder[IO, Author] = jsonEncoderOf[Author]
  implicit val entityDecoder: EntityDecoder[IO, Author] = jsonDecoderOf[Author]

}

object ArticleKeyWord {
  implicit val queryParamEncoder: QueryParamEncoder[ArticleKeyWord] = stringQueryParamEncoder.contramap(_.value)
  implicit val encoder: Encoder[ArticleKeyWord]                     = deriveEncoder[ArticleKeyWord]
  implicit val decoder: Decoder[ArticleKeyWord]                     = deriveDecoder[ArticleKeyWord]
  implicit val queryDecoder: QueryParamDecoder[ArticleKeyWord]      = QueryParamDecoder.stringQueryParamDecoder.map(ArticleKeyWord(_))

}

object ArticleId {
  implicit val queryParamEncoder: QueryParamEncoder[ArticleId] = longQueryParamEncoder.contramap(_.value)
  implicit val encoder: Encoder[ArticleId]                     = Encoder.encodeLong.contramap(_.value)
  implicit val decoder: Decoder[ArticleId]                     = Decoder.decodeLong.map(ArticleId(_))
}

object Article {
  implicit val encoder: Encoder[Article]                 = deriveEncoder[Article]
  implicit val decoder: Decoder[Article]                 = deriveDecoder[Article]
  implicit val entityDecoder: EntityDecoder[IO, Article] = jsonDecoderOf[Article]
  implicit val entityEncoder: EntityEncoder[IO, Article] = jsonEncoderOf[Article]
}

object ArticleDetails {
  implicit val encoder: Encoder[ArticleDetails]                 = deriveEncoder[ArticleDetails]
  implicit val decoder: Decoder[ArticleDetails]                 = deriveDecoder[ArticleDetails]
  implicit val entityDecoder: EntityDecoder[IO, ArticleDetails] = jsonDecoderOf[ArticleDetails]
  implicit val entityEncoder: EntityEncoder[IO, ArticleDetails] = jsonEncoderOf[ArticleDetails]
}

object ArticleUpdate {
  implicit val encoder: Encoder[ArticleUpdate]                 = deriveEncoder[ArticleUpdate]
  implicit val decoder: Decoder[ArticleUpdate]                 = deriveDecoder[ArticleUpdate]
  implicit val entityDecoder: EntityDecoder[IO, ArticleUpdate] = jsonDecoderOf[ArticleUpdate]
  implicit val entityEncoder: EntityEncoder[IO, ArticleUpdate] = jsonEncoderOf[ArticleUpdate]
}
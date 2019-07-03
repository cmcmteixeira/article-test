package elevio.common.model

import java.time.ZonedDateTime

import cats.effect.IO
import elevio.common.httpclient._
import io.circe.{Decoder, Encoder}
import org.http4s.{EntityDecoder, EntityEncoder, QueryParamDecoder, QueryParamEncoder}
import org.http4s.QueryParamEncoder.{longQueryParamEncoder, stringQueryParamEncoder}
import io.circe.generic.semiauto._
import io.circe.java8.time._

case class Title(value: String)          extends AnyVal
case class Version(value: String)        extends AnyVal
case class Author(name: String)          extends AnyVal
case class ArticleKeyWord(value: String) extends AnyVal
case class ArticleId(value: Long)        extends AnyVal
case class ArticleUpdate(id: ArticleId)

case class Article(
    id: ArticleId,
    title: Title,
    keywords: List[ArticleKeyWord]
)

case class ArticleDetails(
    id: ArticleId,
    title: Title,
    author: Author,
    keywords: List[ArticleKeyWord],
    updatedAt: ZonedDateTime,
    createdAt: ZonedDateTime,
    lastPublisher: Option[Author],
    version: Version
) {
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
  implicit val encoder: Encoder[Author]                 = deriveEncoder[Author]
  implicit val decoder: Decoder[Author]                 = deriveDecoder[Author]
  implicit val entityEncoder: EntityEncoder[IO, Author] = jsonEncoderOf[Author]
  implicit val entityDecoder: EntityDecoder[IO, Author] = jsonDecoderOf[Author]

}

object ArticleKeyWord {
  implicit val queryParamEncoder: QueryParamEncoder[ArticleKeyWord] = stringQueryParamEncoder.contramap(_.value)
  implicit val encoder: Encoder[ArticleKeyWord]                     = Encoder.encodeString.contramap(_.value)
  implicit val decoder: Decoder[ArticleKeyWord]                     = Decoder.decodeString.map(ArticleKeyWord(_))
  implicit val queryDecoder: QueryParamDecoder[ArticleKeyWord]      = QueryParamDecoder.stringQueryParamDecoder.map(ArticleKeyWord(_))

}

object ArticleId {
  implicit val queryParamEncoder: QueryParamEncoder[ArticleId] = longQueryParamEncoder.contramap(_.value)
  implicit val encoder: Encoder[ArticleId]                     = Encoder.encodeLong.contramap(_.value)
  implicit val decoder: Decoder[ArticleId]                     = Decoder.decodeLong.map(ArticleId(_))
}

object Version {
  implicit val queryParamEncoder: QueryParamEncoder[Version] = stringQueryParamEncoder.contramap(_.value)
  implicit val encoder: Encoder[Version]                     = Encoder.encodeString.contramap(_.value)
  implicit val decoder: Decoder[Version]                     = Decoder.decodeString.map(Version(_))
}

object Article {
  implicit val encoder: Encoder[Article]                 = deriveEncoder[Article]
  implicit val decoder: Decoder[Article]                 = deriveDecoder[Article]
  implicit val entityDecoder: EntityDecoder[IO, Article] = jsonDecoderOf[Article]
  implicit val entityEncoder: EntityEncoder[IO, Article] = jsonEncoderOf[Article]
}

object ArticleDetails {
  implicit val encoder: Encoder[ArticleDetails] =
    WrappedEntity.encoder[ArticleDetails]("article")(deriveEncoder[ArticleDetails]).contramap(WrappedEntity(_))
  implicit val decoder: Decoder[ArticleDetails]                 = WrappedEntity.decoder[ArticleDetails]("article")(deriveDecoder[ArticleDetails]).map(_.entity)
  implicit val entityDecoder: EntityDecoder[IO, ArticleDetails] = jsonDecoderOf[ArticleDetails]
  implicit val entityEncoder: EntityEncoder[IO, ArticleDetails] = jsonEncoderOf[ArticleDetails]
}

object ArticleUpdate {
  implicit val encoder: Encoder[ArticleUpdate]                 = deriveEncoder[ArticleUpdate]
  implicit val decoder: Decoder[ArticleUpdate]                 = deriveDecoder[ArticleUpdate]
  implicit val entityDecoder: EntityDecoder[IO, ArticleUpdate] = jsonDecoderOf[ArticleUpdate]
  implicit val entityEncoder: EntityEncoder[IO, ArticleUpdate] = jsonEncoderOf[ArticleUpdate]
}

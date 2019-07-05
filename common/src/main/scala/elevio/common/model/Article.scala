package elevio.common.model

import java.time.{Instant, LocalDateTime, ZonedDateTime}

import cats.effect.IO
import elevio.common.httpclient._
import io.circe.Decoder.Result
import io.circe.{Decoder, Encoder, HCursor, Json, JsonObject}
import org.http4s.{EntityDecoder, EntityEncoder, QueryParamDecoder, QueryParamEncoder}
import org.http4s.QueryParamEncoder.{longQueryParamEncoder, stringQueryParamEncoder}
import io.circe.generic.semiauto._
import io.circe.syntax._
import io.circe.generic.encoding.DerivedObjectEncoder
import cats.implicits._

case class Title(value: String)          extends AnyVal
case class Version(value: String)        extends AnyVal
case class Author(value: String)         extends AnyVal
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
    updatedAt: ZonedDateTime, // could manually derive a decoder bummer
    createdAt: ZonedDateTime, // could manually derive a decoder bummer
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
  implicit val encoder: Encoder[Author]                 = Encoder.encodeString.contramap(_.value)
  implicit val decoder: Decoder[Author]                 = Decoder.decodeString.map(Author(_))
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
  implicit val encoder: Encoder[ArticleDetails] = WrappedEntity
    .encoder[ArticleDetails]("article")(
      (a: ArticleDetails) =>
        Json.obj(
          "id"    -> a.id.asJson,
          "title" -> a.title.asJson,
          "author" -> Json.obj(
            "name" -> a.author.asJson
          ),
          "keywords"   -> a.keywords.asJson,
          "updated_at" -> a.updatedAt.asJson,
          "created_at" -> a.createdAt.asJson,
          "last_publisher" -> Json.obj(
            "name" -> a.lastPublisher.asJson
          ),
          "editor_version" -> a.version.asJson,
      )
    )
    .contramap(WrappedEntity(_))
  implicit val decoder: Decoder[ArticleDetails] = WrappedEntity
    .decoder[ArticleDetails]("article")(
      (c: HCursor) =>
        for {
          id                <- c.downField("id").as[ArticleId]
          title             <- c.downField("title").as[Title]
          author            <- c.downField("author").downField("name").as[Author]
          keywords          <- c.downField("keywords").as[List[ArticleKeyWord]]
          updatedAt         <- c.downField("updated_at").as[ZonedDateTime]
          createdAt         <- c.downField("created_at").as[ZonedDateTime]
          lastPublisherJson <- c.downField("last_publisher").as[Option[Json]]
          lastPublisher     <- lastPublisherJson.map(HCursor.fromJson).map(_.downField("name").as[Author].map(_.some)).getOrElse(Right(None))
          version           <- c.downField("editor_version").as[Version]
        } yield ArticleDetails(id, title, author, keywords, updatedAt, createdAt, lastPublisher, version)
    )
    .map(_.entity)
  implicit val entityDecoder: EntityDecoder[IO, ArticleDetails] = jsonDecoderOf[ArticleDetails]
  implicit val entityEncoder: EntityEncoder[IO, ArticleDetails] = jsonEncoderOf[ArticleDetails]
}

object ArticleUpdate {
  implicit val encoder: Encoder[ArticleUpdate]                 = deriveEncoder[ArticleUpdate]
  implicit val decoder: Decoder[ArticleUpdate]                 = deriveDecoder[ArticleUpdate]
  implicit val entityDecoder: EntityDecoder[IO, ArticleUpdate] = jsonDecoderOf[ArticleUpdate]
  implicit val entityEncoder: EntityEncoder[IO, ArticleUpdate] = jsonEncoderOf[ArticleUpdate]
}

package elevio.article.model

import cats.effect.IO
import elevio.article.httpclient._
import io.circe.{Decoder, Encoder}
import org.http4s.{EntityDecoder, EntityEncoder, QueryParamDecoder, QueryParamEncoder}
import org.http4s.QueryParamEncoder.{longQueryParamEncoder, stringQueryParamEncoder}
import io.circe.generic.semiauto._

object Article {
  implicit val encoder: Encoder[Article]                 = deriveEncoder[Article]
  implicit val decoder: Decoder[Article]                 = deriveDecoder[Article]
  implicit val entityDecoder: EntityDecoder[IO, Article] = jsonDecoderOf[Article]
  implicit val entityEncoder: EntityEncoder[IO, Article] = jsonEncoderOf[Article]

}
case class Article(id: ArticleId)
object ArticleId {
  implicit val queryParamEncoder: QueryParamEncoder[ArticleId] = longQueryParamEncoder.contramap(_.value)
  implicit val encoder: Encoder[ArticleId]                     = Encoder.encodeLong.contramap(_.value)
  implicit val decoder: Decoder[ArticleId]                     = Decoder.decodeLong.map(ArticleId(_))
}
case class ArticleId(value: Long) extends AnyVal

object KeyWord {
  implicit val queryParamEncoder: QueryParamEncoder[KeyWord] = stringQueryParamEncoder.contramap(_.value)
  implicit val encoder: Encoder[KeyWord]                     = deriveEncoder[KeyWord]
  implicit val decoder: Decoder[KeyWord]                     = deriveDecoder[KeyWord]
  implicit val queryDecoder: QueryParamDecoder[KeyWord]      = QueryParamDecoder.stringQueryParamDecoder.map(KeyWord(_))

}

case class KeyWord(value: String) extends AnyVal

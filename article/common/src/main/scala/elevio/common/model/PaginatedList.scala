package elevio.common.model

import cats.effect.IO
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe._
import io.circe.Json._
import io.circe.syntax._
import org.http4s.{EntityDecoder, EntityEncoder, QueryParamDecoder}
import elevio.common.httpclient._
import org.http4s.dsl.io.OptionalQueryParamDecoderMatcher

object PageNumber {
  implicit val decoder: Decoder[PageNumber] = Decoder.decodeLong.map(PageNumber(_))
  implicit val encoder: Encoder[PageNumber] = Encoder.encodeLong.contramap(_.value)
}
case class PageNumber(value: Long) extends AnyVal

object PageSize {
  implicit val decoder: Decoder[PageSize] = Decoder.decodeLong.map(PageSize(_))
  implicit val encoder: Encoder[PageSize] = Encoder.encodeLong.contramap(_.value)
}
case class PageSize(value: Long) extends AnyVal

object PageCount {
  implicit val decoder: Decoder[PageCount] = Decoder.decodeLong.map(PageCount(_))
  implicit val encoder: Encoder[PageCount] = Encoder.encodeLong.contramap(_.value)
}
case class PageCount(value: Long) extends AnyVal

object EntriesCount {
  implicit val decoder: Decoder[EntriesCount] = Decoder.decodeLong.map(EntriesCount(_))
  implicit val encoder: Encoder[EntriesCount] = Encoder.encodeLong.contramap(_.value)
}
case class EntriesCount(value: Long) extends AnyVal

object Page {
  implicit val decoder: Decoder[Page]                = Decoder.decodeLong.map(Page(_))
  implicit val encoder: Encoder[Page]                = Encoder.encodeLong.contramap(_.value)
  implicit val queryDecoder: QueryParamDecoder[Page] = QueryParamDecoder.longQueryParamDecoder.map(Page(_))
}
case class Page(value: Long) extends AnyVal

object ItemsPerPage {
  implicit val queryDecoder: QueryParamDecoder[ItemsPerPage] = QueryParamDecoder.intQueryParamDecoder.map(ItemsPerPage(_))

}
case class ItemsPerPage(value: Int) extends AnyVal

trait PaginatedList[A] {
  def elements: List[A]
  def page: Page
  def pageSize: PageSize
  def totalPages: PageCount
  def totalEntries: EntriesCount
}
private object PaginatedList {
  def decoder[A](entity: String)(implicit decoder: Decoder[A]): Decoder[(List[A], Page, PageSize, PageCount, EntriesCount)] =
    (c: HCursor) =>
      for {
        page         <- c.downField("page_number").as[Page]
        pageSize     <- c.downField("page_size").as[PageSize]
        totalPages   <- c.downField("total_pages").as[PageCount]
        totalEntries <- c.downField("total_entries").as[EntriesCount]
        elements     <- c.downField(entity).as[List[A]]
      } yield (elements, page, pageSize, totalPages, totalEntries)

  def encoder[A](entity: String)(implicit decoder: Encoder[A]): Encoder[PaginatedList[A]] =
    paginatedList =>
      Json.obj(
        "page_number"   -> paginatedList.page.asJson,
        "page_size"     -> paginatedList.pageSize.asJson,
        "total_pages"   -> paginatedList.totalPages.asJson,
        "total_entries" -> paginatedList.totalEntries.asJson,
        entity          -> paginatedList.elements.asJson
    )
}

case class ElevioPaginatedList[A](
    elements: List[A],
    page: Page,
    pageSize: PageSize,
    totalPages: PageCount,
    totalEntries: EntriesCount
) extends PaginatedList[A] {
  //a bit hacky but safe as the unnaply will always be defined
  def toServicePaginatedList: ServicePaginatedList[A] = (ServicePaginatedList[A] _).tupled(ElevioPaginatedList.unapply(this).get)
}

object ElevioPaginatedList {
  implicit val decoder: Decoder[ElevioPaginatedList[Article]] =
    PaginatedList.decoder[Article]("articles").map((ElevioPaginatedList[Article] _).tupled)
  implicit val encoder: Encoder[ElevioPaginatedList[Article]]                 = PaginatedList.encoder[Article]("articles").contramap(p => p)
  implicit val entityDecoder: EntityDecoder[IO, ElevioPaginatedList[Article]] = jsonDecoderOf[ElevioPaginatedList[Article]]
  implicit val entityEncoder: EntityEncoder[IO, ElevioPaginatedList[Article]] = jsonEncoderOf[ElevioPaginatedList[Article]]
}
case class ServicePaginatedList[A](
    elements: List[A],
    page: Page,
    pageSize: PageSize,
    totalPages: PageCount,
    totalEntries: EntriesCount
) extends PaginatedList[A]

object ServicePaginatedList {
  implicit val decoder: Decoder[ServicePaginatedList[Article]] =
    PaginatedList.decoder[Article]("articles").map((ServicePaginatedList[Article] _).tupled)
  implicit val encoder: Encoder[ServicePaginatedList[Article]]                 = PaginatedList.encoder[Article]("articles").contramap(p => p)
  implicit val entityDecoder: EntityDecoder[IO, ServicePaginatedList[Article]] = jsonDecoderOf[ServicePaginatedList[Article]]
  implicit val entityEncoder: EntityEncoder[IO, ServicePaginatedList[Article]] = jsonEncoderOf[ServicePaginatedList[Article]]
}

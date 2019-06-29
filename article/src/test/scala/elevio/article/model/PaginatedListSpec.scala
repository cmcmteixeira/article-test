package elevio.article.model

import elevio.article.unit.DefaultSpec
import org.scalatest.{FlatSpec, Matchers}
import io.circe._
import io.circe.syntax._

class PaginatedListSpec extends DefaultSpec {
  "The paginated list" should "correctly decode json" in {
    val elementsKey = "custom_entity"
    case class CustomEntity(foo: Long, bar: String)
    implicit val decoder: Decoder[CustomEntity] = io.circe.generic.semiauto.deriveDecoder[CustomEntity]

    val theEntity    = CustomEntity(1L, "Woah !")
    val elements     = List(theEntity)
    val page         = Page(100L)
    val pageSize     = PageSize(1L)
    val totalPages   = PageCount(2L)
    val totalEntries = EntriesCount(10L)
    val result       = (elements, page, pageSize, totalPages, totalEntries)
    PaginatedList
      .decoder(elementsKey)
      .decodeJson(Json.obj(
        "page_number"   -> Json.fromLong(page.value),
        "page_size"     -> Json.fromLong(pageSize.value),
        "total_pages"   -> Json.fromLong(totalPages.value),
        "total_entries" -> Json.fromLong(totalEntries.value),
        elementsKey -> Json.arr(Json.obj(
          "foo" -> Json.fromLong(theEntity.foo),
          "bar" -> Json.fromString(theEntity.bar)
        ))
      )) shouldBe Right(result)
  }

  "The Service Paginated List" should "correctly decode json" in {
    val article      = Article(ArticleId(1L))
    val page         = Page(100L)
    val pageSize     = PageSize(1L)
    val totalPages   = PageCount(2L)
    val totalEntries = EntriesCount(10L)
    val result       = ServicePaginatedList(List(article), page, pageSize, totalPages, totalEntries)
    ServicePaginatedList.decoder
      .decodeJson(
        Json.obj(
          "page_number"   -> Json.fromLong(page.value),
          "page_size"     -> Json.fromLong(pageSize.value),
          "total_pages"   -> Json.fromLong(totalPages.value),
          "total_entries" -> Json.fromLong(totalEntries.value),
          "articles"      -> Json.arr(article.asJson)
        )) shouldBe Right(result)
  }

  it should "encode json" in {
    val article      = Article(ArticleId(1L))
    val page         = Page(100L)
    val pageSize     = PageSize(1L)
    val totalPages   = PageCount(2L)
    val totalEntries = EntriesCount(10L)
    val result       = ServicePaginatedList(List(article), page, pageSize, totalPages, totalEntries)
    ServicePaginatedList.decoder.decodeJson(result.asJson) should ===(Right(result))
  }

  "The Elevio Paginated List" should "correctly decode json" in {
    val article      = Article(ArticleId(1L))
    val page         = Page(100L)
    val pageSize     = PageSize(1L)
    val totalPages   = PageCount(2L)
    val totalEntries = EntriesCount(10L)
    val result       = ElevioPaginatedList(List(article), page, pageSize, totalPages, totalEntries)
    ElevioPaginatedList.decoder
      .decodeJson(
        Json.obj(
          "page_number"   -> Json.fromLong(page.value),
          "page_size"     -> Json.fromLong(pageSize.value),
          "total_pages"   -> Json.fromLong(totalPages.value),
          "total_entries" -> Json.fromLong(totalEntries.value),
          "articles"      -> Json.arr(article.asJson)
        )) shouldBe Right(result)
  }

  it should "encode json" in {
    val article      = Article(ArticleId(1L))
    val page         = Page(100L)
    val pageSize     = PageSize(1L)
    val totalPages   = PageCount(2L)
    val totalEntries = EntriesCount(10L)
    val result       = ElevioPaginatedList(List(article), page, pageSize, totalPages, totalEntries)
    ElevioPaginatedList.decoder.decodeJson(result.asJson) should ===(Right(result))
  }

  it should "convert items to a ServicePaginatedList" in {
    val article       = Article(ArticleId(1L))
    val page          = Page(100L)
    val pageSize      = PageSize(1L)
    val totalPages    = PageCount(2L)
    val totalEntries  = EntriesCount(10L)
    val paginatedList = ElevioPaginatedList(List(article), page, pageSize, totalPages, totalEntries)
    val result        = ServicePaginatedList(List(article), page, pageSize, totalPages, totalEntries)

    paginatedList.toServicePaginatedList should ===(result)
  }
}

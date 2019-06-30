package elevio.common.model

import elevio.common.unit.DefaultSpec
import org.scalatest.{FlatSpec, Matchers}
import io.circe._
import io.circe.syntax._

class PaginatedListSpec extends DefaultSpec {

  class Fixture {
    val article       = Article(ArticleId(1L), Title("title"), List(ArticleKeyWord("keyword")))
    val page          = Page(100L)
    val pageSize      = PageSize(1L)
    val totalPages    = PageCount(2L)
    val totalEntries  = EntriesCount(10L)
    val elevioPagList = ElevioPaginatedList(List(article), page, pageSize, totalPages, totalEntries)
    val result        = ServicePaginatedList(List(article), page, pageSize, totalPages, totalEntries)
  }

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
    val f = new Fixture
    ServicePaginatedList.decoder
      .decodeJson(
        Json.obj(
          "page_number"   -> Json.fromLong(f.page.value),
          "page_size"     -> Json.fromLong(f.pageSize.value),
          "total_pages"   -> Json.fromLong(f.totalPages.value),
          "total_entries" -> Json.fromLong(f.totalEntries.value),
          "articles"      -> Json.arr(f.article.asJson)
        )) shouldBe Right(f.result)
  }

  it should "encode json" in {
    val f = new Fixture
    ServicePaginatedList.decoder.decodeJson(f.result.asJson) should ===(Right(f.result))
  }

  "The Elevio Paginated List" should "correctly decode json" in {
    val f = new Fixture
    ElevioPaginatedList.decoder
      .decodeJson(
        Json.obj(
          "page_number"   -> Json.fromLong(f.page.value),
          "page_size"     -> Json.fromLong(f.pageSize.value),
          "total_pages"   -> Json.fromLong(f.totalPages.value),
          "total_entries" -> Json.fromLong(f.totalEntries.value),
          "articles"      -> Json.arr(f.article.asJson)
        )) should ===(Right(f.elevioPagList))
  }

  it should "encode json" in {
    val f = new Fixture
    ElevioPaginatedList.decoder.decodeJson(f.elevioPagList.asJson) should ===(Right(f.elevioPagList))
  }

  it should "convert items to a ServicePaginatedList" in {
    val f = new Fixture
    f.elevioPagList.toServicePaginatedList should ===(f.result)
  }
}

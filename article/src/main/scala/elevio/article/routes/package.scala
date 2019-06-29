package elevio.article

import cats.effect.IO
import elevio.article.model.{ItemsPerPage, Page}
import org.http4s._
import org.http4s.dsl.io._

package object routes {
  private[routes] sealed class RouteException(message: String, cause: Throwable)               extends Throwable(message, cause)
  private[routes] case class BadRequestE(message: String, cause: Throwable = null)             extends RouteException(message, cause)
  private[routes] case class NotFoundE(message: String = "Not Found", cause: Throwable = null) extends RouteException(message, cause)

  object PageParamMatcher         extends OptionalQueryParamDecoderMatcher[Page]("page")
  object ItemsPerPageParamMatcher extends OptionalQueryParamDecoderMatcher[ItemsPerPage]("itemsPerPage")

  def defaultExceptionHandler: PartialFunction[Throwable, IO[Response[IO]]] = {
    case BadRequestE(message, _) => BadRequest(message)
    case NotFoundE(message, _)   => NotFound(message)
  }
}

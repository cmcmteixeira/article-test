package elevio.article

import cats.effect.IO
import elevio.article.model.{ApiKey, JWT}
import io.circe.{Decoder, Encoder}
import org.http4s.{EntityDecoder, EntityEncoder, Header, Headers, Request, Response, Status}

package object httpclient {
  def jsonEncoderOf[A](implicit enc: Encoder[A]): EntityEncoder[IO, A] = org.http4s.circe.jsonEncoderOf[IO, A](implicitly, enc)
  def jsonDecoderOf[A](implicit dec: Decoder[A]): EntityDecoder[IO, A] = org.http4s.circe.jsonOf[IO, A](implicitly, dec)
  def handle404[A](response: Response[IO])(implicit decoder: EntityDecoder[IO, A]): IO[Option[A]] =
    if (response.status == Status.NotFound) {
      IO(None)
    } else {
      response.as[A].map(Some(_))
    }

  implicit class AuthRequest(req: Request[IO]) {
    def withAuthentication(jwt: JWT, apiKey: ApiKey): Request[IO] =
      req.withHeaders(
        Header("Authorization", s"${jwt.value}"),
        Header("X-api-key", s"${apiKey.value}")
      )
  }
}

package elevio.updater.health

import cats.effect.IO
import com.itv.bucky.AmqpClient
import doobie.util.transactor.Transactor
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import doobie.implicits._
import elevio.common.httpclient.{jsonDecoderOf, jsonEncoderOf}
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import org.http4s.{EntityDecoder, EntityEncoder}

case class Unhealthy(amqp: Boolean)
object Unhealthy {
  implicit val decoder: Decoder[Unhealthy]                 = deriveDecoder[Unhealthy]
  implicit val encoder: Encoder[Unhealthy]                 = deriveEncoder[Unhealthy]
  implicit val entityDecoder: EntityDecoder[IO, Unhealthy] = jsonDecoderOf[Unhealthy]
  implicit val entityEncoder: EntityEncoder[IO, Unhealthy] = jsonEncoderOf[Unhealthy]
}

trait HealthCheckService {
  def health(): IO[Either[Unhealthy, Unit]]
}

object HealthCheckService extends StrictLogging {

  def apply(amqp: AmqpClient[IO]): HealthCheckService = new HealthCheckService {
    def health(): IO[Either[Unhealthy, Unit]] =
      for {
        _          <- IO(logger.info("Running healthcheck"))
        amqpHealth <- amqp.isConnectionOpen
        _          <- IO(logger.info(s"Amqp health: {}", amqpHealth))
      } yield {
        if (amqpHealth) {
          ().asRight
        } else {
          Unhealthy(amqpHealth).asLeft
        }
      }
  }
}

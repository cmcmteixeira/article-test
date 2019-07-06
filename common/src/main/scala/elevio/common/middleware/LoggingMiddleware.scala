package elevio.common.middleware

import cats.data.{Kleisli, OptionT}
import cats.effect.{IO, Sync}
import com.typesafe.scalalogging.StrictLogging
import org.http4s.HttpRoutes

object LoggingMiddleware extends StrictLogging {

  def apply(service: HttpRoutes[IO]): HttpRoutes[IO] =
    Kleisli(request =>
      OptionT {
        for {
          _             <- IO.delay(logger.info(s"Received request for URL: ${request.uri.toString()}}"))
          resultAttempt <- service.run(request).value.attempt
          _ <- IO.delay(
            resultAttempt.fold[Unit](
              e => logger.error("Error processing request.", e),
              maybeResponse =>
                maybeResponse.fold[Unit](
                  logger.warn(s"Not match found for request. Will return 404")
                )(response =>
                  if (response.status.isSuccess) {
                    logger.info(s"Request processe with status code ${response.status.code}")
                  } else {
                    logger.warn(s"Request processe with status code ${response.status.code}")
                })
            ))
          result <- IO.fromEither(resultAttempt)
        } yield result
    })

}

package elevio.service.routes

import cats.effect.IO
import cats.implicits._
import elevio.service.health.{HealthCheckService, Unhealthy}
import elevio.service.unit._
import org.http4s.client.Client
import org.http4s.implicits._
import org.mockito.Mockito._

class AppStatusRoutesTest extends DefaultSpec {
  class Fixture {
    val healthCheckService: HealthCheckService = mock[HealthCheckService]
    val appStatusRoutes: AppStatusRoutes       = new AppStatusRoutes(healthCheckService)
    val client: Client[IO]                     = Client.fromHttpApp(appStatusRoutes.routes.orNotFound)
  }

  "The App status route" should "return a 200 if the service is healthy" in withIO {
    val f = new Fixture
    when(f.healthCheckService.health()).thenReturn(IO(Right(())))

    for {
      response <- f.client.get("/health")(_.status.code.pure[IO])
    } yield {
      response should ===(200)
    }
  }

  it should "return a 500 if the service is unhealthy" in withIO {
    val f = new Fixture
    when(f.healthCheckService.health()).thenReturn(IO(Left(Unhealthy(false, true))))

    for {
      response <- f.client.get("/health")(_.status.code.pure[IO])
    } yield {
      response should ===(500)
    }
  }

}

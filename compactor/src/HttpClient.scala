import cats.effect.{IO, Resource}
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

import scala.concurrent.duration.Duration

object HttpClient:

  val resource: Resource[IO, Client[IO]] =
    EmberClientBuilder
      .default[IO]
      .withIdleConnectionTime(Duration.Inf)
      .build

  def apply(): Resource[IO, Client[IO]] = resource

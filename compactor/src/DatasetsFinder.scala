import cats.effect.IO
import io.circe.Decoder
import io.circe.Decoder.decodeList
import org.http4s.Method.GET
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.{Headers, Request}

import scala.concurrent.duration.Duration

object DatasetsFinder:

  def findDatasets(using config: Config): IO[List[String]] =
    EmberClientBuilder
      .default[IO]
      .withIdleConnectionTime(Duration.Inf)
      .build
      .use {
        _.run {
          Request[IO](GET, config.adminApi / "datasets", headers = Headers(config.admin.asAuthHeader))
        }.use(_.as[List[String]])
      }

  private given Decoder[List[String]] =
    val dsName: Decoder[String] = _.downField("ds.name").as[String]
    _.downField("datasets").as(decodeList(dsName)).map(_.map(_.substring(1)))

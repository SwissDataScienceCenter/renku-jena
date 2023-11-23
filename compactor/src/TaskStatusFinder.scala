import cats.effect.IO
import cats.syntax.all.*
import io.circe.Decoder
import org.http4s.Method.GET
import org.http4s.Status.Ok
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.{Headers, Request, Response}

import java.time.{Duration, Instant}

object TaskStatusFinder:

  def hasFinished(taskId: TaskId)(using config: Config): IO[Either[String, Option[Duration]]] =
    HttpClient().use {
      _.run {
        Request[IO](GET, config.adminApi / "tasks" / taskId.value, headers = Headers(config.admin.asAuthHeader))
      }.use(toDuration(taskId))
    }

  private def toDuration(taskId: TaskId): Response[IO] => IO[Either[String, Option[Duration]]] = {
    case resp if resp.status == Ok =>
      given Decoder[Either[String, Option[Duration]]] = decoder(taskId)
      resp.as[Either[String, Option[Duration]]]
    case resp =>
      resp.as[String].map(_.trim.asLeft)
  }

  private def decoder(taskId: TaskId): Decoder[Either[String, Option[Duration]]] = cur =>
    (
      cur.downField("started").as[Option[Instant]],
      cur.downField("finished").as[Option[Instant]],
      cur.downField("success").as[Option[Boolean]]
    ).mapN {
      case (None, _, _)                      => s"Task = $taskId not found".asLeft
      case (Some(started), _, Some(false))   => s"Task = $taskId failed".asLeft
      case (Some(started), maybeFinished, _) => maybeFinished.map(Duration.between(started, _)).asRight
    }

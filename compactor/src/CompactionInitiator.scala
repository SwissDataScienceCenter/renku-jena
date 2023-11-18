import cats.effect.IO
import cats.syntax.all.*
import io.circe.Decoder
import org.http4s.Method.POST
import org.http4s.Status.Ok
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.{EntityDecoder, Headers, MediaType, Request}

object CompactionInitiator:

  def kickOffCompaction(ds: String)(using config: Config): IO[Either[String, TaskId]] =
    HttpClient().use {
      _.run {
        Request[IO](POST,
                    (config.adminApi / "compact" / ds).withQueryParam("deleteOld", true),
                    headers = Headers(config.admin.asAuthHeader)
        )
      }.use {
        case resp if resp.status == Ok && resp.contentType.exists(_.mediaType == MediaType.application.json) =>
          resp.as[TaskId].map(_.asRight)
        case resp =>
          import EntityDecoder.text
          resp.as[String].map(_.trim.asLeft)
      }
    }

  private given Decoder[TaskId] =
    _.downField("taskId").as[String].map(TaskId.apply)

final case class TaskId(value: String):
  override lazy val toString: String = value

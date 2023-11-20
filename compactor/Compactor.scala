//> using scala 3
//> using dep org.http4s::http4s-circe::0.23.24
//> using dep org.http4s::http4s-dsl::0.23.24
//> using dep org.http4s::http4s-ember-client::0.23.24
//> using dep org.slf4j:slf4j-simple:2.0.9
//> using dep org.typelevel::cats-core::2.10.0
//> using resourceDir src

import cats.effect.{ExitCode, IO, IOApp, Temporal}
import cats.syntax.all.*

import java.time.Duration as JDuration
import scala.concurrent.duration.*

object Compactor extends IOApp:

  def run(args: List[String]): IO[ExitCode] =
    Config.fromConfig
      .fold(ifEmpty = IO.println("Admin credentials not found; compacting skipped.")) { config =>
        given Config = config
        for {
          datasets <- DatasetsFinder.findDatasets
          _        <- datasets.map(compact).sequence
          _        <- IO.println("Compacting finished.")
        } yield ()
      }
      .as(ExitCode.Success)

  private def compact(dataset: String)(using config: Config) =
    CompactionInitiator.kickOffCompaction(dataset) >>= {
      case Left(err) =>
        IO.println(s"Compacting '$dataset' failed; $err")
      case Right(taskId) =>
        IO.println(s"Compacting '$dataset' started; taskId = $taskId") >>
          waitToFinish(taskId, dataset)
    }

  private def waitToFinish(taskId: TaskId, dataset: String)(using config: Config): IO[Unit] =
    TaskStatusFinder.hasFinished(taskId) >>= {
      case Left(err)             => IO.println(err)
      case Right(None)           => Temporal[IO].delayBy(waitToFinish(taskId, dataset), 2.seconds)
      case Right(Some(duration)) => IO.println(s"Compacting '$dataset' done in ${makeReadable(duration)}")
    }

  private lazy val makeReadable: JDuration => String = {
    case duration if duration.toMillis < 3 * 60  => s"${duration.toMillis}ms"
    case duration if duration.toSeconds < 3 * 60 => s"${duration.toSeconds}s"
    case duration                                => s"${duration.toMinutes}min"
  }

//> using scala 3
//> using dep eu.timepit::fs2-cron-calev::0.8.3
//> using dep org.apache.shiro:shiro-core:1.13.0
//> using dep org.http4s::http4s-circe::0.23.24
//> using dep org.http4s::http4s-dsl::0.23.24
//> using dep org.http4s::http4s-ember-client::0.23.24
//> using dep org.slf4j:slf4j-simple:2.0.9
//> using dep org.typelevel::cats-core::2.10.0
//> using resourceDir src

import cats.effect.{ExitCode, IO, IOApp, Temporal}
import cats.syntax.all.*
import com.github.eikek.calev.CalEvent
import eu.timepit.fs2cron.calev.CalevScheduler
import fs2.Stream
import org.typelevel.log4cats.Logger

import java.time.Duration as JDuration
import scala.concurrent.duration.*

object Compactor extends IOApp with Logging:

  def run(args: List[String]): IO[ExitCode] =
    Config.readConfig
      .fold(
        err => Logger[IO].warn(s"Compacting skipped/failed: $err"),
        config => schedule(compactDatasets(using config), config.schedule)
      )
      .as(ExitCode.Success)

  private def schedule(task: IO[Unit], schedule: CalEvent) =
    val scheduler = CalevScheduler.systemDefault[IO]
    (scheduler.awakeEvery(schedule) >> Stream.eval(task)).compile.drain

  private def compactDatasets(using config: Config) =
    for {
      datasets <- DatasetsFinder.findDatasets
      _        <- datasets.map(compact).sequence
      _        <- Logger[IO].info("Compacting finished.")
    } yield ()

  private def compact(dataset: String)(using config: Config) =
    CompactionInitiator.kickOffCompaction(dataset) >>= {
      case Left(err) =>
        Logger[IO].error(s"Compacting '$dataset' failed; $err")
      case Right(taskId) =>
        Logger[IO].info(s"Compacting '$dataset' started; taskId = $taskId") >>
          waitToFinish(taskId, dataset)
    }

  private def waitToFinish(taskId: TaskId, dataset: String)(using config: Config): IO[Unit] =
    TaskStatusFinder.hasFinished(taskId) >>= {
      case Left(err)             => Logger[IO].error(err)
      case Right(None)           => Temporal[IO].delayBy(waitToFinish(taskId, dataset), 2.seconds)
      case Right(Some(duration)) => Logger[IO].info(s"Compacting '$dataset' done in ${makeReadable(duration)}")
    }

  private lazy val makeReadable: JDuration => String = {
    case duration if duration.toMillis < 3 * 60  => s"${duration.toMillis}ms"
    case duration if duration.toSeconds < 3 * 60 => s"${duration.toSeconds}s"
    case duration                                => s"${duration.toMinutes}min"
  }

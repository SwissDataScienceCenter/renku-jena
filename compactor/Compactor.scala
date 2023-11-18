//> using scala 3
//> using dep org.http4s::http4s-circe::0.23.24
//> using dep org.http4s::http4s-dsl::0.23.24
//> using dep org.http4s::http4s-ember-client::0.23.24
//> using dep org.slf4j:slf4j-simple:2.0.9
//> using dep org.typelevel::cats-core::2.10.0
//> using resourceDir src

import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all.*

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
    CompactionInitiator.kickOffCompaction(dataset).flatMap {
      case Left(err)     => IO.println(s"Compacting '$dataset' failed; $err")
      case Right(taskId) => IO.println(s"Compacting '$dataset' started; taskId = $taskId")
    }

//> using scala 3
//> using dep org.http4s::http4s-circe::0.23.24
//> using dep org.http4s::http4s-dsl::0.23.24
//> using dep org.http4s::http4s-ember-client::0.23.24
//> using dep org.slf4j:slf4j-simple:2.0.9
//> using dep org.typelevel::cats-core::2.10.0
//> using resourceDir src

import cats.effect.{ExitCode, IO, IOApp}

object Compactor extends IOApp:

  def run(args: List[String]): IO[ExitCode] =
    Config.fromConfig
      .map { config =>
        given Config = config
        DatasetsFinder.findDatasets.flatMap(r => IO.println(s"Compacting done $r"))
      }
      .getOrElse(IO.println("Admin credentials not found; compacting skipped."))
      .as(ExitCode.Success)

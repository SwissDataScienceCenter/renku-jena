//> using scala 3
//> using dep org.http4s::http4s-client::0.23.24
//> using dep org.typelevel::cats-core::2.10.0
//> using resourceDir src

import cats.effect.{ExitCode, IO, IOApp}

object Compactor extends IOApp:

  def run(args: List[String]): IO[ExitCode] =
    Config.fromConfig
      .map { config =>
        IO.println(s"Compacting done $config")
      }
      .getOrElse(IO.println("Admin credentials not found; compacting skipped."))
      .as(ExitCode.Success)

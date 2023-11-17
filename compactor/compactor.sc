#!/usr/bin/env -S scala-cli shebang

//> using scala 3
//> using dep org.typelevel::cats-core::2.10.0
//> using resourceDir src

val maybeConfig = Config.fromConfig

println(maybeConfig)

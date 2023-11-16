#!/usr/bin/env -S scala-cli shebang

sys.env.map{case (k, v) => println(s"env: $k -> $v") }

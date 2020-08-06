package io.github.mvillafuertem

import io.github.mvillafuertem.AkkaZioConfiguration.program
import zio.console.Console
import zio.{ ExitCode, URIO }

object AkkaZioApplication extends zio.App {

  override def run(args: List[String]): URIO[Any with Console, ExitCode] = program

}

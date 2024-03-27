package ru.mardaunt.chat.server

import cats.effect.{ExitCode, IO, IOApp}

object ChatServerApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    IO(ExitCode.Success)
  }

}

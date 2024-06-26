package ru.mardaunt.chat.client

import cats.effect.Async
import cats.effect.std.Console
import fs2.Stream

trait InputStream[F[_]] {
  def read: Stream[F, String]
}

object InputStream {

  def apply[F[_]: Async: Console](bufSize: Int): InputStream[F] = new InputStream[F] {

    override def read: Stream[F, String] =
        fs2.io.stdinUtf8(bufSize)
          .through(fs2.text.lines)
          .evalTap(deduplicateMessage)
          .filter(_.nonEmpty)

      private def deduplicateMessage: PartialFunction[String, F[Unit]] =
        _ => Console[F].print("\u001b[1A\u001b[0K")

    }

}

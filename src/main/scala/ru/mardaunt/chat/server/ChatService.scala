package ru.mardaunt.chat.server

import cats.effect.Concurrent
import cats.effect.std.Console
import fs2.concurrent.Topic
import fs2.Stream
import io.grpc.Metadata
import ru.mardaunt.chat.{ChatServiceFs2Grpc, Events}

object ChatService {

  def apply[F[_]: Concurrent: Console](eventsTopic: Topic[F, Events]): ChatServiceFs2Grpc[F, Metadata] =
    new ChatServiceFs2Grpc[F, Metadata] {

      val eventsToClients: Stream[F, Events] =
        eventsTopic
          .subscribeUnbounded
          .evalTap(data => Console[F].println(s"From topic: $data"))

      override def eventsStream(eventsFromClient: Stream[F, Events], ctx: Metadata): Stream[F, Events] = {
        eventsToClients.concurrently(
          eventsFromClient
            .evalTap(event => Console[F].println(s"Event from client: $event"))
            .evalMap(eventsTopic.publish1)
        )
      }

    }

}

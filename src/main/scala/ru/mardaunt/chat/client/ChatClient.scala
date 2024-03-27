package ru.mardaunt.chat.client

import cats.effect.Concurrent
import cats.effect.std.Console
import fansi.{Bold, Color}
import io.grpc.Metadata
import ru.mardaunt.chat.{ChatServiceFs2Grpc, Events}
import fs2.{Pipe, Stream}
import ru.mardaunt.chat.Events.Event.{ClientLogin, ClientLogout, ClientMessage, ServerShutdown}
import ru.mardaunt.chat.Events.{Login, Message}

trait ChatClient[F[_]] {
  def start: F[Unit]
}

object ChatClient {

  def apply[F[_]: Concurrent: Console](
                                      clientName: String,
                                      inputStream: InputStream[F],
                                      chatService: ChatServiceFs2Grpc[F, Metadata]
                                      ): ChatClient[F] = new ChatClient[F] {

    private val grpcMetadata = new Metadata()

    override def start: F[Unit] =
      chatService
        .eventsStream(
          login(clientName) ++ inputStream.read.through(inputToEvent),
          grpcMetadata
        )
        .through(processEventsFromServer)
        .through(writeToConsole)
        .compile
        .drain


    private def login(clientName: String): Stream[F, Events] =
      Stream(Events(ClientLogin(Login(clientName))))

    private def inputToEvent: Pipe[F, String, Events] =
      _.map { text =>
        Events(
          ClientMessage(
            Message(name = clientName, message = text)
          )
        )
      }

    private def processEventsFromServer: Pipe[F, Events, String] =
      _.map { data =>
        data.event match {
          case event: ClientLogin => s"${Color.Green(event.value.name).overlay(Bold.On)} entered the chat."
          case event: ClientLogout => s"${Color.Blue(event.value.name).overlay(Bold.On)} left the chat."
          case event: ClientMessage => s"${Color.LightGray(s"${event.value.name}:").overlay(Bold.On)} ${event.value.message}"
          case _: ServerShutdown => s"${Color.LightRed("Server shutdown")}"
          case unknown => s"${Color.Red("Unknown event:")} $unknown"
        }
      }

    private def writeToConsole: Pipe[F, String, Nothing] =
      _.foreach(Console[F].println)

  }

}

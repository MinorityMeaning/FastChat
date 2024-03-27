package ru.mardaunt.chat.client

import cats.effect.{ExitCode, IO, IOApp, Resource}
import io.grpc.{Channel, Metadata}
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import fs2.grpc.syntax.all.fs2GrpcSyntaxManagedChannelBuilder
import ru.mardaunt.chat.ChatServiceFs2Grpc

object ChatClientApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    resources.use { chatServiceFs2Grpc =>
      ChatClient(
        args.headOption.getOrElse("Anonymous"),
        InputStream[IO](bufSize = 1024),
        chatServiceFs2Grpc
      ).start
    }.as(ExitCode.Success)


  private def buildChatService(channel: Channel): Resource[IO, ChatServiceFs2Grpc[IO, Metadata]] =
    ChatServiceFs2Grpc.stubResource[IO](channel)

  private def resources: Resource[IO, ChatServiceFs2Grpc[IO, Metadata]] =
    NettyChannelBuilder
      .forAddress("127.0.0.1", 60065)
      .usePlaintext
      .resource[IO]
      .flatMap(buildChatService(_))

}

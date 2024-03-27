package ru.mardaunt.chat.server

import cats.effect.{ExitCode, IO, IOApp, Resource}
import fs2.concurrent.Topic
import io.grpc.ServerServiceDefinition
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import fs2.grpc.syntax.all._
import ru.mardaunt.chat.{ChatServiceFs2Grpc, Events}

import java.util.concurrent.TimeUnit

object ChatServerApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = for {
    topic <- Topic[IO, Events]
    serviceResource: Resource[IO, ServerServiceDefinition] =
      ChatServiceFs2Grpc.bindServiceResource[IO](ChatService(topic))
    _ <- serviceResource.use(runServer)
  } yield ExitCode.Success

  private def runServer(service: ServerServiceDefinition): IO[Nothing] =
    NettyServerBuilder
      .forPort(60065)
      .keepAliveTime(5, TimeUnit.SECONDS)
      .addService(service)
      .resource[IO]
      .evalMap(server => IO(server.start))
      .useForever

}

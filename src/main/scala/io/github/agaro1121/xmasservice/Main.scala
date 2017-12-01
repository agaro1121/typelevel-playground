package io.github.agaro1121.xmasservice

import cats.effect.IO
import io.github.agaro1121.xmasservice.repo.InMemoryUserRepo
import io.github.agaro1121.xmasservice.service.UserService
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.util.StreamApp



object Main extends StreamApp[IO] with UserService[IO] {

  override def userRepo = InMemoryUserRepo

  override def stream(args: List[String], requestShutdown: IO[Unit]) = {
    val builder =
      BlazeBuilder[IO]
        .bindHttp(9000, "localhost") //not necessary if you're just binding to 8080 <- default
        .mountService(userRoutes, "/")


    builder.serve
  }
}

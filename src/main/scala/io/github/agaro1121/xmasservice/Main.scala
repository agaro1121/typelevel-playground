package io.github.agaro1121.xmasservice

import cats.{Functor, Id}
import cats.effect.IO
import io.github.agaro1121.xmasservice.repo.{InMemoryUserRepo, UserRepo}
import io.github.agaro1121.xmasservice.service.UserRoutes
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.util.StreamApp

object Main extends StreamApp[IO] with UserRoutes with InMemoryUserRepo {


  override def userRepo[F[_]] = this

  override def stream(args: List[String], requestShutdown: IO[Unit]) = {
    val builder: BlazeBuilder[IO] =
      BlazeBuilder[IO]
        .bindHttp(9000, "localhost") //not necessary if you're just binding to 8080 <- default
        .mountService(userRoutes, "/")


    builder.serve
  }
}

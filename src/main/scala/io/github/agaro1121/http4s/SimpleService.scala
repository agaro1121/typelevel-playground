package io.github.agaro1121.http4s

import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import cats.implicits._
import org.http4s.server.Server
import org.http4s.util.{ExitCode, StreamApp}
import org.http4s.server.blaze._

import org.http4s.implicits._

object SimpleService extends StreamApp[IO] {


  override def stream(args: List[String], requestShutdown: IO[Unit]) = {
    val helloWorldService: HttpService[IO] = HttpService[IO] {
      case GET -> Root / "hello" / name =>
        Ok(s"Hello, $name.")
    }

    val builder: BlazeBuilder[IO] =
      BlazeBuilder[IO]
        .bindHttp(9000, "localhost") //not necessary if you're just binding to 8080 <- default
        .mountService(helloWorldService, "/")


    builder.serve
  }

}

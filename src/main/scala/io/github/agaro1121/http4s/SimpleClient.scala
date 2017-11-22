package io.github.agaro1121.http4s

import cats.effect.IO

import org.http4s.client.blaze._

object SimpleClient extends App {

  val httpClient = PooledHttp1Client[IO]()

  httpClient
    .expect[String]("http://localhost:9000/hello/Batman")
    .unsafeRunAsync(println)

  scala.io.StdIn.readLine()
}

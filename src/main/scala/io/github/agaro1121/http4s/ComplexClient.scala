package io.github.agaro1121.http4s

import cats.effect.IO
import org.http4s.client.blaze.PooledHttp1Client
import io.circe.generic.auto._
import org.http4s.circe._

object ComplexClient extends App {

  val httpClient = PooledHttp1Client[IO]()

  httpClient
    .expect("http://localhost:9000/tweets/5")(jsonOf[IO, Tweet])
    .unsafeRunAsync(println)

  httpClient
    .expect("http://localhost:9000/tweets/popular")(jsonOf[IO, Seq[Tweet]])
    .unsafeRunAsync(println)

  scala.io.StdIn.readLine()

}

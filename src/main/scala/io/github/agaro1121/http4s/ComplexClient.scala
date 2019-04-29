package io.github.agaro1121.http4s

import cats.effect.{ExitCode, IO, IOApp}
import io.circe.generic.auto._
import org.http4s.circe._
import org.http4s.client.blaze.BlazeClientBuilder

object ComplexClient extends IOApp {

//  val httpClient = BlazeClientBuilder[IO](concurrent.ExecutionContext.global).stream

  val runner = for {
    client <- BlazeClientBuilder[IO](concurrent.ExecutionContext.global).stream
  } yield {
    client
      .expect("http://localhost:9000/tweets/5")(jsonOf[IO, Tweet])
      .unsafeRunAsync(println)

    client
      .expect("http://localhost:9000/tweets/popular")(jsonOf[IO, Seq[Tweet]])
      .unsafeRunAsync(println)
  }

  scala.io.StdIn.readLine()

  override def run(args: List[String]): IO[ExitCode] =
    runner.compile.drain.map(_ => ExitCode.Success)
}

package io.github.agaro1121.http4s

import cats.effect._
import org.http4s._
import io.circe.syntax._
import io.circe.generic.auto._
import org.http4s.circe._
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.implicits._
import org.http4s.dsl.io._
import cats.implicits._

case class Tweet(id: Int, message: String)
object Tweet {
  def getTweet(tweetId: Int): IO[Tweet] = IO.pure(Tweet(tweetId, "Sample"))
  def getPopularTweets(): IO[Seq[Tweet]] = IO.pure(Seq(Tweet(5, "List Sample"), Tweet(10, "Another!")))
}


object ComplexService extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    val tweetService = HttpRoutes.of[IO] {

      case GET -> Root / "tweets" =>
        Ok(Tweet.getPopularTweets().map(_.asJson))

      case GET -> Root / "tweets" / IntVar(tweetId) =>
        Tweet.getTweet(tweetId).map(_.asJson).flatMap(Ok(_))

    }

    val httpApp = Router(
      "/" -> tweetService
    ).orNotFound

    val builder =
      BlazeServerBuilder[IO]
        .bindHttp(9000, "0.0.0.0") //not necessary if you're just binding to 8080 <- default
        .withHttpApp(httpApp)


    builder.serve
      .compile
      .drain
      .as(ExitCode.Success) //sugar for .map(_ => ExitCode.Success)

  }
}

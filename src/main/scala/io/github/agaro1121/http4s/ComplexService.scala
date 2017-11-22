package io.github.agaro1121.http4s

import cats.effect.IO
import org.http4s.util.StreamApp
import org.http4s._
import org.http4s.dsl.io._
import io.circe.syntax._
import io.circe.generic.auto._
import org.http4s.circe._
import org.http4s.server.blaze.BlazeBuilder

case class Tweet(id: Int, message: String)
object Tweet {
  def getTweet(tweetId: Int): IO[Tweet] = IO.pure(Tweet(tweetId, "Sample"))
  def getPopularTweets(): IO[Seq[Tweet]] = IO.pure(Seq(Tweet(5, "List Sample")))
}


object ComplexService extends StreamApp[IO] {
  override def stream(args: List[String], requestShutdown: IO[Unit]) = {

    val tweetService  = HttpService[IO] {

      case GET -> Root / "tweets" / "popular" =>
        Ok(Tweet.getPopularTweets().map(_.asJson))

      case GET -> Root / "tweets" / IntVar(tweetId) =>
        Tweet.getTweet(tweetId).map(_.asJson).flatMap(Ok(_))

    }

    val builder: BlazeBuilder[IO] =
      BlazeBuilder[IO]
        .bindHttp(9000, "localhost") //not necessary if you're just binding to 8080 <- default
        .mountService(tweetService, "/")


    builder.serve
  }

}

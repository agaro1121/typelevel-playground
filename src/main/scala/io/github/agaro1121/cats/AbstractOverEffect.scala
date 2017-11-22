package io.github.agaro1121.cats

import cats.Id
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

trait UptimeService[F[_]] {
  def getUptime: F[Int]
}

trait RealUptimeService extends UptimeService[Future]
trait TestUptimeService extends UptimeService[Id]

class UptimeClient {
  def doStuff[F[_]](uptimeService: UptimeService[F]): F[Int] =
    uptimeService.getUptime
}

object UptimeServiceTester extends App {

  println(
    Await.result(
      new UptimeClient().doStuff(new RealUptimeService {
        override def getUptime: Future[Int] = Future.successful(5)
      }),
      1 second
    )
  )

  println(
    new UptimeClient().doStuff[Id](new TestUptimeService {
      override def getUptime: Id[Int] = 5
    })
  )


}
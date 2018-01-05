package io.github.agaro1121.cats

import cats.{Id, Monad}
import cats.effect.IO

import scala.concurrent.Future

trait SomeService[F[_]] {

  def doStuff[T](id: String)(implicit M: Monad[F]): F[T]

}

trait TestService extends SomeService[Id]
trait RealService extends SomeService[Future]
trait TaskService extends SomeService[IO]

object RealServiceImpl extends RealService {
  override def doStuff[T](id: String)(implicit M: Monad[Future]): Future[T] =
    M.pure(???)
}
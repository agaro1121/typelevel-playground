package io.github.agaro1121.taglessfinal2

import cats.{Monad, MonadError, StackSafeMonad}
import io.github.agaro1121.taglessfinal.{DatabaseError, User}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait DBAlgebra[F[_], T] {

  def create(t: T): F[Boolean]
  def read(id: Long)(implicit ev: MonadError[F, DatabaseError]): F[T]
  def delete(id: Long)(implicit ev: MonadError[F, DatabaseError]): F[Unit]

}


object DBAlgebra {

  object FutureInterpreter extends DBAlgebra[Future, User] {
    override def create(t: User): Future[Boolean] =
      Future.successful(false)

    override def read(id: Long)(implicit ev: MonadError[Future, DatabaseError]): Future[User] =
      Future.successful(User(1, "Bob", 31))

    override def delete(id: Long)(implicit ev: MonadError[Future, DatabaseError]): Future[Unit] =
      Future.successful(Unit)
  }

}

class Program[F[_]: Monad](DB: DBAlgebra[F, User]) {

  import cats.syntax.flatMap._
  import cats.syntax.functor._

  def updateUser(user: User)(implicit ev: MonadError[F, DatabaseError]): F[Boolean] = for {
      user <- DB.read(user.id)
      sss <- DB.create(user.copy(name = "Bobby"))
    } yield sss

}

object TestAllThisThis extends App {

  implicit object m extends MonadError[Future, DatabaseError]
    with StackSafeMonad[Future] {
      override def pure[A](x: A): Future[A] = Future.successful(x)
      override def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = fa.flatMap(f)
      override def raiseError[A](e: DatabaseError): Future[A] = Future.failed(e)
      override def handleErrorWith[A](fa: Future[A])(f: DatabaseError => Future[A]): Future[A] =
        fa.recoverWith{ case t:DatabaseError => f(t) }
  }

  val program = new Program(DBAlgebra.FutureInterpreter)
  println(Await.result(program.updateUser(User(1, "Bob", 31)), 1.second))

}
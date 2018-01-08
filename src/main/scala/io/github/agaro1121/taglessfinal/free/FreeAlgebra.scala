package io.github.agaro1121.taglessfinal.free

import cats.free.Free
import cats.data.{EitherK, EitherT}
import cats.{InjectK, ~>}
import cats.instances.future._

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import DBFreeAlgebraT._
import io.github.agaro1121.taglessfinal.free.Combined.DbAndConsoleAlgebra
import io.github.agaro1121.taglessfinal.free.ConsoleFreeAlgebraT.ConsoleFreeAlgebraTI
import io.github.agaro1121.taglessfinal.{DatabaseError, ErrorDeletingUser, ErrorFindingUser, User}

sealed trait ConsoleFreeAlgebraT[T]
case class PutLine[T](t: T) extends ConsoleFreeAlgebraT[Unit]

object ConsoleFreeAlgebraT {
  type ConsoleAlgebra[T] = Free[ConsoleFreeAlgebraT, T]

  class ConsoleFreeAlgebraTI[F[_]](implicit I: InjectK[ConsoleFreeAlgebraT, F]) {
    def putLine[T](t: T): Free[F, Unit] = Free.inject[ConsoleFreeAlgebraT, F](PutLine(t))
  }

  implicit def consoleFreeAlgebraTI[F[_]](implicit I: InjectK[ConsoleFreeAlgebraT, F]): ConsoleFreeAlgebraTI[F] =
    new ConsoleFreeAlgebraTI[F]

  val FutureInterpreter: (ConsoleFreeAlgebraT ~> Future) = new (ConsoleFreeAlgebraT ~> Future) {
    override def apply[A](fa: ConsoleFreeAlgebraT[A]): Future[A] =
      fa match {
        case PutLine(t) =>
          Future.successful(
            println(t)
          ).asInstanceOf[Future[A]]
      }
  }

}

sealed trait DBFreeAlgebraT[T]
case class Create[T](t: T) extends DBFreeAlgebraT[Boolean]
case class Read[T](id: Long) extends DBFreeAlgebraT[Either[DatabaseError, T]]
case class Delete[T](id: Long) extends DBFreeAlgebraT[Either[DatabaseError, Unit]]


object DBFreeAlgebraT {
  type DBFreeAlgebra[T] = Free[DBFreeAlgebraT, T]

  class DBFreeAlgebraTI[F[_]](implicit I: InjectK[DBFreeAlgebraT, F]) {
    def create[T](t: T): Free[F, Boolean] =
      Free.inject[DBFreeAlgebraT, F](Create(t))

    def read[T](id: Long): Free[F, Either[DatabaseError, User]] =
      Free.inject[DBFreeAlgebraT, F](Read(id))

    def delete[T](id: Long): Free[F, Either[DatabaseError, Unit]] =
      Free.inject[DBFreeAlgebraT, F](Delete(id))
  }

  implicit def dBFreeAlgebraTI[F[_]](implicit I: InjectK[DBFreeAlgebraT, F]): DBFreeAlgebraTI[F] =
    new DBFreeAlgebraTI[F]

  val FutureInterpreter = new (DBFreeAlgebraT ~> Future) {
    val users: mutable.Map[Long, User] = mutable.Map.empty

    override def apply[A](fa: DBFreeAlgebraT[A]): Future[A] =
      fa match {
        case Create(user) =>
          val inserted = users.put(user.asInstanceOf[User].id, user.asInstanceOf[User])
          Future.successful(inserted.isEmpty || inserted.isDefined).map(_.asInstanceOf[A])
        case Read(id) =>
          Future.successful(users.get(id).toRight(ErrorFindingUser)).map(_.asInstanceOf[A])
        case Delete(id) => {
          import cats.syntax.either._
          val deleted = users.remove(id)
          Future.successful(
            deleted.fold(ErrorDeletingUser(s"User with Id($id) was not there").asLeft[Unit])(_ => Right(()))
          ).map(_.asInstanceOf[A])
        }
      }
  }

}

object Combined {
  type DbAndConsoleAlgebra[T] = EitherK[DBFreeAlgebraT, ConsoleFreeAlgebraT, T]

  val FutureInterpreter: DbAndConsoleAlgebra ~> Future =
    DBFreeAlgebraT.FutureInterpreter or ConsoleFreeAlgebraT.FutureInterpreter
}

class FreeUserRepo(implicit
                   DB: DBFreeAlgebraTI[Combined.DbAndConsoleAlgebra],
                   C: ConsoleFreeAlgebraTI[Combined.DbAndConsoleAlgebra]) {

  def getUser(id: Long): Free[DbAndConsoleAlgebra, Either[DatabaseError, User]] = DB.read(id)
  def addUser(user: User): Free[DbAndConsoleAlgebra, Boolean] = DB.create(user)

  type DbAndConsoleAlgebraContainer[A] = Free[DbAndConsoleAlgebra, A]

  def updateUser(user: User): Free[DbAndConsoleAlgebra, Either[DatabaseError, Boolean]] = (for {
    userFromDB <- EitherT(getUser(user.id))
    _ <- EitherT.liftF(C.putLine(s"We found user($userFromDB)!!"))
    successfullyAdded <- EitherT.liftF[DbAndConsoleAlgebraContainer, DatabaseError, Boolean](addUser(user))
  } yield successfullyAdded).value

}


object DBFreeAlgebraRunner extends App {

  val repo = new FreeUserRepo

  println(Await.result(
    (for {
      _ <- repo.addUser(User(1, "Bob", 31))
      dbErrorOrSuccessfullyUpdated <- repo.updateUser(User(1, "Bobby", 31))
    } yield dbErrorOrSuccessfullyUpdated).foldMap(Combined.FutureInterpreter),
    1 second))

}
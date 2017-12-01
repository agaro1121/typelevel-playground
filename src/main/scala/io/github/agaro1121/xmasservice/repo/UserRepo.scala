package io.github.agaro1121.xmasservice.repo

import cats.Id
import cats.data.{EitherT, Ior}
import cats.effect.IO
import io.github.agaro1121.xmasservice.models.User
import cats.syntax.either.catsSyntaxEitherId
import cats.syntax.ior.catsSyntaxIorId

import scala.collection.mutable
import scala.language.higherKinds

trait UserRepo[F[_]] {

  def addUser(user: User): F[RepoResult]

  def getUserByFirstName(firstName: String): F[Either[UserRepoError, User]]
  def getUserByLastName(lastName: String): F[Either[UserRepoError, User]]
  def getUserById(id: String): F[Either[UserRepoError, User]]
  //TODO: Can I paginate? Stream? :-)
  def getUsersByBirthday(birthday: String): F[Ior[UserRepoError, Seq[User]]]
  //TODO: Can I paginate? Stream? :-)
  def getAllUsers: F[Ior[UserRepoError, Seq[User]]]

  def updateUser(user: User): F[RepoResult]

  def deleteUser(id: String): F[RepoResult]

}

object UserRepo {
  val inMemoryUserRepo: UserRepo[IO] = InMemoryUserRepo
  val staticUserRepo: UserRepo[Id] = StaticUserRepo
}

object StaticUserRepo extends UserRepo[Id] {

  val dummyUser = User(
    id = "someId",
    firstName = "Anthony",
    lastName = "Garo",
    birthday = "1988Nov21",
    ts = "someTimeStamp"
  )

  override def addUser(user: User) = Success

  override def getUserByFirstName(firstName: String) = dummyUser.asRight

  override def getUserByLastName(lastName: String) = UserNotFound.asLeft //User.dummyUser.asRight

  override def getUserById(id: String) = dummyUser.asRight

  override def getUsersByBirthday(birthday: String) = List(dummyUser).rightIor

  override def getAllUsers = List(dummyUser).rightIor

  override def updateUser(user: User) = Success

  override def deleteUser(id: String) = Failure(s"Failed to delete user $id")
}

object InMemoryUserRepo extends UserRepo[IO] {

  private var users = Vector.empty[User]

  override def addUser(user: User) = {
    users = users :+ user
    IO.pure(Success)
  }

  override def getUserByFirstName(firstName: String) = {
    users
      .find(user => user.firstName == firstName)
      .fold(IO.pure(UserRepoError.userNotFound.asLeft[User]))(
        user => IO.pure(user.asRight[UserRepoError]))
  }

  override def getUserByLastName(lastName: String) = {
    users
      .find(user => user.lastName == lastName)
      .fold(IO.pure(UserRepoError.userNotFound.asLeft[User]))(
        user => IO.pure(user.asRight[UserRepoError]))
  }

  override def getUserById(id: String) = {
    users
      .find(user => user.id == id)
      .fold(IO.pure(UserRepoError.userNotFound.asLeft[User]))(
        user => IO.pure(user.asRight[UserRepoError]))
  }

  override def getUsersByBirthday(birthday: String) = {
    IO.pure(
      users
        .filter(user => user.birthday == birthday)
        .rightIor
    )
  }

  override def getAllUsers = IO.pure(users.toList.rightIor[UserRepoError])

  //TODO: implement these two properly
  override def updateUser(user: User) = IO.pure(Success)

  override def deleteUser(id: String) = IO.pure(Failure("Too Lazy to implement"))
}
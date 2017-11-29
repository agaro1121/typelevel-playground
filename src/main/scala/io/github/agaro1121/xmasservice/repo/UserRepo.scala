package io.github.agaro1121.xmasservice.repo

import cats.Id
import cats.data.Ior
import io.github.agaro1121.xmasservice.models.User
import cats.syntax.either.catsSyntaxEitherId
import cats.syntax.ior.catsSyntaxIorId
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
//  val inMemoryUserRepo: UserRepo[Id] = InMemoryUserRepo
}

trait InMemoryUserRepo extends UserRepo[Id] {

  override def addUser(user: User) = Success

  override def getUserByFirstName(firstName: String) = User.dummyUser.asRight

  override def getUserByLastName(lastName: String) = UserNotFound.asLeft //User.dummyUser.asRight

  override def getUserById(id: String) = User.dummyUser.asRight

  override def getUsersByBirthday(birthday: String) = List(User.dummyUser).rightIor

  override def getAllUsers = List(User.dummyUser).rightIor

  override def updateUser(user: User) = Success

  override def deleteUser(id: String) = Failure(s"Failed to delete user $id")
}


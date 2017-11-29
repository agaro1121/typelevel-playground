package io.github.agaro1121.xmasservice.service

import cats.{Functor, Id}
import cats.data.EitherT
import cats.effect.IO
import io.github.agaro1121.xmasservice.models.User
import io.github.agaro1121.xmasservice.repo.{Failure, InMemoryUserRepo, Success, UserRepo}
import org.http4s.HttpService
import org.http4s.dsl.impl.Root
import org.http4s.dsl.io._
import org.http4s.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.syntax.functor.toFunctorOps

import scala.language.higherKinds

// TODO: convert this to a class?
trait UserRoutes {

  def userRepo[F[_]]: UserRepo[F]

  val userRoutes = HttpService[IO] {

    case POST -> Root / "user" / User(user) =>
      userRepo.addUser(user).map {
        case Success => Ok()
        case Failure(msg) => InternalServerError(msg)
      }

    case GET -> Root / "users" / "firstname" / firstName =>
      EitherT(userRepo
        .getUserByFirstName(firstName))
        .fold(
          error => InternalServerError(error.asJson),
          user => Ok(user.asJson)
        )

    case GET -> Root / "users" / "lastname" / lastName =>
      userRepo
        .getUserByLastName(lastName)
        .fold(
          error => NotFound(error.asJson),
          user => Ok(user.asJson)
        )

    case GET -> Root / "users" / "id" / id =>
      userRepo
        .getUserById(id)
        .fold(
          error => NotFound(error.asJson),
          user => Ok(user.asJson)
        )

    case GET -> Root / "users" / "birthday"/ birthday =>
      userRepo
        .getUsersByBirthday(birthday)
        .fold(
          error => NotFound(error.asJson),
          users => Ok(users.asJson),
          (error, users) => InternalServerError(s"$error while searching for $users")
        )

    case GET -> Root / "users" =>
      userRepo.getAllUsers
        .fold(
          error => NotFound(error.asJson),
          users => Ok(users.asJson),
          (error, users) => InternalServerError(s"$error while searching for $users")
        )

    case PUT -> Root / "users" / User(user) =>
      userRepo.updateUser(user) match {
        case Success => Ok()
        case Failure(msg) => InternalServerError(msg)
      }

    case DELETE -> Root / "users" / id =>
      userRepo.deleteUser(id) match {
        case Success => Ok()
        case Failure(msg) => InternalServerError(msg)
      }

  }

}

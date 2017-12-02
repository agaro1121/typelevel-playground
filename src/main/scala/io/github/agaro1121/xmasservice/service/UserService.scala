package io.github.agaro1121.xmasservice.service

import cats.{Eval, Monad}
import cats.data.Ior
import cats.effect.Effect
import io.github.agaro1121.xmasservice.models.User
import io.github.agaro1121.xmasservice.repo.{Failure, Success, UserRepo}
import org.http4s.HttpService
import io.circe.syntax._
import io.circe.generic.auto._
import org.http4s.circe._
import scala.language.higherKinds
import cats.syntax.flatMap._
import org.http4s.dsl.Http4sDsl

trait UserService[F[_]] extends Http4sDsl[F] {

  def userRepo: UserRepo[F]

  implicit def decoder(implicit GG: Monad[F], eff: Effect[F]) = jsonOf[F, User]

  def userRoutes(implicit GG: Monad[F], eff: Effect[F]) = HttpService[F] {

    case req @ POST -> Root / "user" => {
      req.as[User].flatMap { user =>
          userRepo.addUser(user).flatMap {
            case Success => Ok()
            case Failure(msg) => InternalServerError(msg)
          }
      }
    }

    case GET -> Root / "users" / "firstname" / firstName => {
      userRepo
        .getUserByFirstName(firstName)
        .flatMap {
          case Left(error) => InternalServerError(error.asJson)
          case Right(user) => Ok(user.asJson)
        }
    }

    case GET -> Root / "users" / "lastname" / lastName =>
      userRepo
        .getUserByLastName(lastName)
        .flatMap{
          case Left(error) => NotFound(error.asJson)
          case Right(user) => Ok(user.asJson)
        }

    case GET -> Root / "users" / "id" / id =>
      userRepo
        .getUserById(id)
        .flatMap{
          case Left(error) => NotFound(error.asJson)
          case Right(user) => Ok(user.asJson)
        }

    case GET -> Root / "users" / "birthday"/ birthday =>
      userRepo
        .getUsersByBirthday(birthday)
          .flatMap{
            case Ior.Left(error) => NotFound(error.asJson)
            case Ior.Right(user) => Ok(user.asJson)
            case Ior.Both(error, users) => InternalServerError(s"$error while searching for $users")
          }

    case GET -> Root / "users" =>
      userRepo.getAllUsers
        .flatMap {
          case Ior.Left(error) => NotFound(error.asJson)
          case Ior.Right(users) => Ok(users.asJson)
          case Ior.Both(error, users) => InternalServerError(s"$error while searching for $users")
        }

    case req @ PUT -> Root / "users" => {
//      implicit val decoder = jsonOf[F, User] //TODO: dupe
      req.as[User].flatMap { user =>
        userRepo.updateUser(user).flatMap {
          case Success => Ok()
          case Failure(msg) => InternalServerError(msg)
        }
      }
    }

    case DELETE -> Root / "users" / id =>
      userRepo.deleteUser(id).flatMap {
        case Success => Ok()
        case Failure(msg) => InternalServerError(msg)
      }

  }

}

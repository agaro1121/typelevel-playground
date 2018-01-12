package io.github.agaro1121.xmasservice.service

import cats.effect.IO
import io.github.agaro1121.xmasservice.repo.{InMemoryUserRepo, UserRepo}
import org.http4s.{Request, Response}
import org.scalatest.{Matchers, WordSpecLike}
import io.circe.generic.auto.exportEncoder
import io.circe.syntax.EncoderOps
import io.github.agaro1121.xmasservice.models.User
import org.http4s.circe.jsonEncoder
import org.http4s.client.dsl.io.http4sWithBodySyntax

class UserServiceTest extends UserService[IO]
  with WordSpecLike
  with Matchers {

  override def userRepo: UserRepo[IO] = InMemoryUserRepo

  "UserService" should {
    "respond OK for searching for a user by firstName" in {

      val dummyUser = User("someid", "anthony", "garo", "birthday", "ts")

      val createNewUser: Request[IO] = POST(uri("/user"), dummyUser.asJson).unsafeRunSync()

      val postResponse: Response[IO] = userRoutes.orNotFound.run(createNewUser).unsafeRunSync
      postResponse.status shouldBe Ok

      val getUserByFirstName: Request[IO] = Request[IO](uri = uri("/users/firstname/anthony"))
      val getResponse: Response[IO] = userRoutes.orNotFound.run(getUserByFirstName).unsafeRunSync

      getResponse.status shouldBe Ok
    }
  }
}

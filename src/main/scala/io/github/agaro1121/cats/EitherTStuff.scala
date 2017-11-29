package io.github.agaro1121.cats

import cats.data.EitherT
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import cats.instances.future._

object EitherTStuff {

  val myOption: Option[Int] = None

  val myOptionET: EitherT[Future, String, Int] =
    EitherT.fromOption[Future](myOption, "option not defined")

  val myOptionETValue: Future[Either[String, Int]] = myOptionET.value

  val myOptionETF: EitherT[Future, String, Int] =
    EitherT.fromOptionF(Future.successful(myOption), "future option not defined")

  val myOptionETFValue: Future[Either[String, Int]] = myOptionETF.value



}

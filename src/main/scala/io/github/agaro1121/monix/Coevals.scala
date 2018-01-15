package io.github.agaro1121.monix

import monix.eval.Coeval
import monix.eval.Coeval.Attempt

import scala.util.{Failure, Success}

/*
* Can replace lazy val
* Can replace by-name params
* Similar to Cats Eval
* Can be used as replacement for IO Monad
* */
object Coevals extends App {

  // lazy
  val coeval = Coeval {
    println("Effect!")
    "Hello!"
  }

  // stuff is happening
  // can throw exceptions
  println(coeval.value)

  coeval.runTry match {
    case Success(value) =>
      println(value)
    case Failure(ex) =>
      System.err.println(ex)
  }

  val alwaysRun = Coeval.eval{ println("Effect!"); "Hello!"}
  alwaysRun.value
  alwaysRun.value
  // evalOnce - memoized
  // defer - Build a factory of Coevals

}

/*
* Replacement for Try[_]
* combinators on Attempt[_] are lazy vs Try[_] which are eager
* */
object Attempts extends App {

  val coeval1 = Coeval(1 + 1)

  val result1: Attempt[Int] = coeval1.runAttempt
  // result1 = Now(2)

  val coeval2 = Coeval.raiseError[Int](new RuntimeException("Hello!"))

  val result2: Attempt[Int] = coeval2.runAttempt

  println(result1.get)
  println(result2.isError)

}
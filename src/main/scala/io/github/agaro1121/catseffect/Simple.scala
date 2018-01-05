package io.github.agaro1121.catseffect

import cats.effect.{IO, _}

import scala.io.StdIn

object IOPlayground extends App {

  def putLine(s: String): IO[Unit] = IO(println(s))
  def getLine(): IO[String] = IO(StdIn.readLine())
  def getNum(): IO[String] = IO(StdIn.readLine())

  val program = for {
    _ <- putLine("Saluton Mondo!")
    _ <- putLine("Enter your name: ")
    name <- getLine()
    _ <- putLine(s"Hi $name !!!")
  } yield ()


  program.unsafeRunSync()

}

object IOPlaygroundWithExceptions extends App {

  def putLine(s: String): IO[Unit] = IO(println(s))
  def getInt(): IO[Int] = IO(StdIn.readInt())

  val program = for {
    _ <- putLine("Saluton Mondo!")
    _ <- putLine("Enter your num: ")
    num <- getInt()
    _ <- putLine(s"Hi $num !!!")
  } yield ()


  program.unsafeRunAsync{
    case Left(err) => println(s"problem getting your number: ${err.getMessage}")
    case _ =>
  }

}

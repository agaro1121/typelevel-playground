package io.github.agaro1121.cats

object MonoidStuff extends App {

  import cats.Monoid
  import cats.Semigroup
  import cats.instances.int._
  import cats.instances.option._


  private val one = Option(1)
  private val two = Option(2)

  println(Monoid[Option[Int]].combine(one, two))
  println(Semigroup[Option[Int]].combine(one, two))

  import cats.syntax.semigroup._
  println(one |+| two)

  import cats.syntax.semigroupk._
  println(one <+> two) //orElse

}

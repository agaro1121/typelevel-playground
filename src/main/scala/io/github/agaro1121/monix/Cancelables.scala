package io.github.agaro1121.monix

import monix.execution.Cancelable
import monix.execution.cancelables._

object Cancelables extends App {

  val c = Cancelable(() => println("cancelled!"))
  c.cancel()

  // allows you to query for cancellation status
  val bc = BooleanCancelable(() => println("Effect!"))
  println(bc.isCanceled)
  bc.cancel()
  println(bc.isCanceled)

  val cc = CompositeCancelable()
  val c1: Cancelable = Cancelable(() => println("Canceled #1"))
  cc += c1 // add
  cc += Cancelable(() => println("Canceled #2"))
  cc += Cancelable(() => println("Canceled #3"))
  cc -= c1 // remove
  cc.cancel()
  cc += Cancelable(() => println("Canceled #4"))

  val sc = SerialCancelable()
  sc := Cancelable(() => println("Canceled sc#1"))
  sc := Cancelable(() => println("Canceled sc#2")) //cancels previous one as soon as you add here
  sc := Cancelable(() => println("Canceled sc#3"))
  sc.cancel() //cancels current. Will cancel anything that follows
  sc := Cancelable(() => println("Canceled sc#4"))

  val rcc = RefCountCancelable { () =>
    println("Everything was canceled")
  }

  val rc1 = rcc.acquire()
  val rc2 = rcc.acquire()

  rcc.cancel()

  // This is now true, but the callback hasn't been invoked yet
  println(rcc.isCanceled)

  val ref3 = rcc.acquire()
  println(ref3 == Cancelable.empty)

  /*
  * Need to cancel all acquired refs AND
  * main Cancelable for things to happen
  * */
  rc1.cancel()
  rc2.cancel()

}

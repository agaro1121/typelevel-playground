package io.github.agaro1121.monix

import monix.execution.Scheduler

import scala.io.StdIn
import concurrent.duration._

object SchedulerApp extends App {

  import monix.execution.Scheduler.Implicits.global

  import concurrent.Future
  Future(1 + 1).foreach(println)

  global.scheduleOnce(2 seconds) {
    println("Saluton Mondo")
  }

  val cancellable = global.scheduleWithFixedDelay(3.seconds, 5.seconds) {
    println("Fixed delay task")
  }

  global.scheduleOnce(30 seconds){
    cancellable.cancel()
  }

  println(global.currentTimeMillis())
  println(System.currentTimeMillis())

  println(global.executionModel)

  StdIn.readLine()
}

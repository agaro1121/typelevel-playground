package io.github.agaro1121.monix

import monix.reactive.Observable

import concurrent.duration._
import monix.execution.Scheduler.Implicits.global

import scala.io.StdIn

object Observables extends App {

  val source = Observable.interval(1 second)
    .filter(_ % 2 == 0)
    .flatMap(x => Observable(x, x))
    .take(10)

  val cancelable = source.dump("0").subscribe()

StdIn.readLine()
}

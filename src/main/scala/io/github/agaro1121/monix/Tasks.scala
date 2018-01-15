package io.github.agaro1121.monix

import monix.eval.{Callback, Coeval, Task}
import monix.execution.CancelableFuture
import monix.execution.Scheduler.Implicits.global

import scala.util.{Failure, Success, Try}
import concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

object Tasks extends App {

  // happens on another thread
  // lazy - only a description
  // not memoized
  val task = Task{ 1 + 1}.delayExecution(200 milliseconds)

  val cancelable = task.runOnComplete{
    case Success(value) =>
      println(value)
    case Failure(ex) =>
      System.out.println(s"ERROR: ${ex.getMessage}")
  }

  val cancelable2 = task.runOnComplete(
    new Callback[Int] {
      def onSuccess(value: Int): Unit =
        println(value)
      def onError(ex: Throwable): Unit =
        System.err.println(s"ERROR: ${ex.getMessage}")
    }
  )

  val future: CancelableFuture[Int] = task.runAsync

  future.foreach(println)

  task.foreach(println) // runs task

  for(result <- task)(println(result)) //runs task

  // if you use the apply function here,
  // the result would print what's on the left side
  val tryingNow: Coeval[Either[CancelableFuture[Int], Int]] = Task.eval(1+1).coeval

  tryingNow.value match {
    case Left(future) =>
      // No luck, this Task really wants async execution
      future.foreach(r => println(s"Async: $r"))
    case Right(result) =>
      println(s"Got lucky: $result")
  }

  StdIn.readLine()
}

object Tasks2 extends App {

  // constructors
  val task: Task[String] = Task.now{println("Effect"); "Hello!"}
  task.foreach(println)

  val taskDelayed: Task[String] = Task.eval{println("Effect"); "Hello!"}
  taskDelayed.runAsync.foreach(println)

  val taskMemoizedOnFirstRun = Task.evalOnce { println("Effect"); "Hello!" }
  taskMemoizedOnFirstRun.runAsync.foreach(println)
  taskMemoizedOnFirstRun.runAsync.foreach(println) // will not print "Effect"

  val async: Task[Int] = Task.create[Int]{ (scheduler, cb) =>
    scheduler.scheduleOnce(1 second)(cb(Try(5)))
  }

  async.foreach(println)


StdIn.readLine()
}

object TasksWithFutures extends App {
  // behaves like normal Future or Task.evalOnce
  val future = Future { println("Effect"); "Hello!" }
  val task = Task.fromFuture(future)
  task.runAsync.foreach(println)
  task.runAsync.foreach(println)// will not print "Effect"

  println("-----")
  // prints "Effect" Hello! every time
  // behaves like normal Task
  val taskDeferred = Task.defer{
    val future = Future { println("Effect"); "Hello!" }
    Task.fromFuture(future)
  }
  taskDeferred.runAsync.foreach(println)
  taskDeferred.runAsync.foreach(println)

  // shortcut for the above
  val taskDeferredViaShortcut = Task.deferFuture {
    Future { println("Effect"); "Hello!" }
  }

  def sumFuture(list: Seq[Int])(implicit ec: ExecutionContext): Future[Int] =
    Future(list.sum)

  def sumTask(list: Seq[Int]): Task[Int] =
    Task.deferFutureAction { _/*Already have a scheduler in scope but this would be implicit scheduler => */ =>
      sumFuture(list)
  }

  sumTask(List(1,2,3)).foreach(println)

  StdIn.readLine()

}

object TaskParallel extends App {

  val locationTask: Task[String] = Task("location")
  val phoneTask: Task[String] = Task.deferFuture(Future("phone"))
  val addressTask = Task.eval("address")


  val serial = for{
    l <- locationTask
    p <- phoneTask
    a <- addressTask
  } yield l + p + a + "gotcha!"

  serial.foreach(println)

  val agg = Task.zipMap3(locationTask, phoneTask, addressTask){
    (l, p, a) => l + p + a + "gotcha!"
  }

  agg.foreach(println)

  // maybe parallel?
  val gatherUnordered = Task.gatherUnordered(Seq(locationTask, phoneTask, addressTask))

  gatherUnordered.foreach(println)

  StdIn.readLine()
}
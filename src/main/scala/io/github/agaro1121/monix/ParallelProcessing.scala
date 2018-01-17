package io.github.agaro1121.monix

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.{Consumer, Observable}

object ParallelProcessing1 extends App {

  val items = 0 until 100

  val tasks = items.map(i => Task{
    println(Thread.currentThread().getName)
    i * 2
  })

  val aggregate = Task.gather(tasks).map(_.toList)

  aggregate.foreach(println)

}

object ParallelProcessing2 extends App {

  val items = 0 until 100

  val tasks = items.map(i => Task{
    println(Thread.currentThread().getName)
    i * 2
  })

  // only difference
  val aggregate = Task.gatherUnordered(tasks).map(_.toList)

  aggregate.foreach(println)

}

object ParallelWithLimits1 extends App {

  val items = 0 until 100

  val tasks = items.map(i => Task(i * 2))

  // builds batches of 10
  val batches = tasks.sliding(10, 10).map(b => Task.gather(b))

  val aggregate = Task.sequence(batches).map(_.flatten.toList)

  aggregate.foreach(println)

}

object ParallelWithLimits2 extends App {

  val source: Observable[List[Long]] = Observable.range(0,1000).bufferIntrospective(256)

  val batched = source.flatMap{ items =>

    val tasks = items.map(i => Task(i * 2))

    val batches = tasks.sliding(10,10).map(b => Task.gather(b))

    val aggregate = Task.sequence(batches).map(_.flatten)

    Observable.fromTask(aggregate)
        .flatMap(i => Observable.fromIterator(i))
  }

}

object ParallelWithLimits3 extends App {

  val source: Observable[Long] = Observable.range(0,1000)

  // does NOT maintain order
  val processed = source.mapAsync(parallelism = 10) {
    i => Task(i * 2)
  }

  processed.toListL.foreach(println)

}

object ParallelWithLimits4 extends App {

  val source: Observable[Long] = Observable.range(0,1000)

  // the observable streams emitted by the source get subscribed in parallel
  val processed = source.mergeMap { i =>
    Observable.fork(Observable.eval(i *   2))
  }

  processed.toListL.foreach(println)

}

object ParallelWithLimits5 extends App {

  val sumConsumer = Consumer.foldLeft[Long, Long](0L)(_ + _)

  val loadBalancer = {
    Consumer.loadBalance(parallelism = 10, sumConsumer)
      .map(_.sum)
  }

  val observable: Observable[Long] = Observable.range(0, 1000)

  val task: Task[Long] = observable.consumeWith(sumConsumer)

  task.runAsync.foreach(println)

}

package io.github.agaro1121.cats.effect

import cats.effect.IO

object IOStuff extends App {


  /*
  * We don’t need to worry about the difference between def and val anymore,
  * because IO is referentially transparent.
  * So you use def when you need parameters,
  * and you use val when you don’t,
  * and you don’t have to think about evaluation semantics.
  * No more subtle bugs caused by accidentally memoizing your effects!
  * */
  val readString = IO { scala.io.StdIn.readLine }

  /*
  * But there is an implication here that is quite profound:
  * IO cannot eagerly evaluate its effects,
  * and similarly cannot memoize its results! If IO were to eagerly evaluate or to memoize,
  * then we could no longer replace references to the expression with the expression itself
  * */
  val program = for {
    _ <- IO { println("Saluton Mondo from the depths of IO !!!! What's your name?") }
    name <- readString
    _ <- IO { println(s"Well hello there, $name")}
  } yield ()

  /*
  * You should only call this function once,
  * ideally at the very end of your program!
  *
  * Any expression involving unsafeRunSync() is not referentially transparent
  * */
  program.unsafeRunSync()

}

object AsnycIOStuff extends App {

  trait Response[T] {
    def onError(t: Throwable): Unit
    def onSuccess(t: T): Unit
  }
  /*
  * Clearly, sendBytes and receiveBytes both represent side-effects,
  * but they’re different than println and readLine in that they don’t
  * produce their results in a sequentially returned value.
  * Instead, they take a callback, Response, which will eventually
  * be notified (likely on some other thread!) when the result is available
  * */
  trait Channel {
    def sendBytes(chunk: Array[Byte], handler: Response[Unit]): Unit
    def receiveBytes(handler: Response[Array[Byte]]): Unit
  }

  def send(c: Channel, chunk: Array[Byte]): IO[Unit] = {
    IO.async{ cb =>
      c.sendBytes(chunk, new Response[Unit] {
        override def onError(t: Throwable): Unit = cb(Left(t))
        override def onSuccess(t: Unit): Unit = cb(Right())
      })
    }
  }

  def receive(c: Channel, chunk: Array[Byte]): IO[Array[Byte]] = {
    IO.async{ cb =>
      c.receiveBytes(new Response[Array[Byte]] {
        override def onError(t: Throwable): Unit = cb(Left(t))
        override def onSuccess(t: Array[Byte]): Unit = cb(Right(chunk))
      })
    }
  }



}
# Free Monad vs Tagless Final

In this post, I will be using Cats as my library of choice for the Free Monad implementation.

# TODO: Write preface && Warning
I'm going to preface this by saying I'm not expert but I find this stuff interesting and wanted to write about it.

This might be a lengthy post so I'll jump right in.
I will start by creating a simple api based on futures.
Then I will create 2 DSLs, interpreters for both, and then show how to mix multiple algebras.
I will compare the Free Monad approach and tagless final approach at each step.


### Free Monads
A technique for encoding your intent as data types and then folding over those data types to build a final structure that will be interpreted into a known Monad of your choice.
The Free Monad itself isn't a Monad but enables you to borrow Monadic properties from other well known Monads.
The [Cats](https://typelevel.org/cats/datatypes/freemonad.html) documentation says it better than I do:
> Concretely, it is just a clever construction that allows us to build a very simple Monad from any functor


### Tagless Final
The best way I can describe this technique is abstracting over your monad of choice.
Just to be clear, this is a pattern of coding and can be achieved mostly using vanilla Scala.
From my understanding, it's called "tagless" because you don't need runtime tags to express constraints on the types of your algebra.
Apparently the term "tagless final" is kind of a pun on the long journey to get to a point where runtime tags were no longer required.
Here is a [better explanation](http://okmij.org/ftp/tagless-final/index.html):
> The so-called `typed tagless final' ...for representing typed higher-order languages in a typed metalanguage[embedded DSL],
 along with type-preserving interpretation, compilation and partial evaluation. 
 The approach is an alternative to the traditional, 
 or 'initial' encoding of an object language as a (generalized) algebraic data type [GADT]. 
 ... 
 The final encoding represents all and only typed object terms 
 without resorting to generalized algebraic data types(GADTs), 
 dependent or other fancy types. 
 The final encoding lets us add new language forms and interpretations without breaking the existing terms and interpreters.
 
Ok, now on to the code...

Your standard build.sbt file:
```
scalacOptions ++= Seq(
  "-Ypartial-unification"
)
libraryDependencies += "org.typelevel" %% "cats-core" % "1.0.1"
libraryDependencies += "org.typelevel" %% "cats-free" % "1.0.1"
```

Imports:
```scala
import cats.data.{EitherK, EitherT}
import cats.free.Free
import cats.{InjectK, Monad, ~>}
import cats.implicits._

import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
```

Some common models:
```scala 
case class User(id: Long, name: String, age: Int)

class DatabaseError extends Throwable
case object ErrorFindingUser extends DatabaseError
case object ErrorUpdatingUser extends DatabaseError
case class ErrorDeletingUser(msg: String) extends DatabaseError
```

The initial api we will be converting:
```scala
trait Database[T] {
  def create(t: T): Future[Boolean]
  def read(id: Long): Future[Either[DatabaseError, T]]
  def delete(id: Long): Future[Either[DatabaseError, Unit]]
}
```

Ok so far so good...

## Step 1 - Create your DSLs

### Tagless Final
```scala
trait DatabaseAlgebra[F[_], T] {
  def create(t: T): F[Boolean]
  def read(id: Long): F[Either[DatabaseError, T]]
  def delete(id: Long): F[Either[DatabaseError, Unit]]
}
```
This isn't so bad. We just took the future out and moved it up to the trait declaration in the form of `F[_]`
Now instead of everything returning `Future[_]`, it returns `F[_]` and `F` can be whatever Monad you want.
For example: `Future`, `IO`, `Id`, `Task`, etc... 

## Free
```scala
import cats.free.Free

/** This is your ADT - specifically, a GADT for the astute reader ;-) */
sealed trait DBFreeAlgebraT[T]
case class Create[T](t: T) extends DBFreeAlgebraT[Boolean]
case class Read[T](id: Long) extends DBFreeAlgebraT[Either[DatabaseError, T]]
case class Delete[T](id: Long) extends DBFreeAlgebraT[Either[DatabaseError, Unit]]
/********************************************************/

object DBFreeAlgebraT {
  type DBFreeAlgebra[T] = Free[DBFreeAlgebraT, T]

  // Smart constructors  
  def create[T](t: T): DBFreeAlgebra[Boolean] =
    Free.liftF[DBFreeAlgebraT, Boolean](Create(t))

  def read[T](id: Long): DBFreeAlgebra[Either[DatabaseError, T]] =
    Free.liftF[DBFreeAlgebraT, Either[DatabaseError, T]](Read(id))

  def delete[T](id: Long): DBFreeAlgebra[Either[DatabaseError, Unit]] =
    Free.liftF[DBFreeAlgebraT, Either[DatabaseError, Unit]](Delete(id))
}
```

Whoooo! Step 1 is done! We have our DSLs.
As you can see the Free implementation has a little more boilerplate.
Free is encoding your algebra as ADTs so we need all the case classes.
We also need the smart constructors because our case classes are too specific.
Smart constructors create instances of our case classes but the return type is the more generalized super type: `DBFreeAlgebraT[T]`.
These will help the scala compiler with implicit lookups later.

Now, as it stands none of this does anything useful.
We need to create some interpreters to interpret our algebra into actions.

# Step 2 - Create Interpreters
- For this section we'll create interpreters to execute our actions using `Future`s

### Tagless Final
```scala
object DatabaseAlgebra {

  val FutureInterpreter: DatabaseAlgebra[Future, User] =
    new DatabaseAlgebra[Future, User] {
      val users: mutable.Map[Long, User] = mutable.Map.empty

      override def create(user: User): Future[Boolean] = {
        val inserted = users.put(user.id, user)
        Future.successful(inserted.isEmpty || inserted.isDefined)
      }

      override def read(id: Long): Future[Either[DatabaseError, User]] =
        Future.successful(users.get(id).toRight(ErrorFindingUser))

      override def delete(id: Long): Future[Either[DatabaseError, Unit]] = {
        import cats.syntax.either._ // for the .asLeft[]. This is also another smart constructor to help the compiler along.
        val deleted = users.remove(id)
        Future.successful(
          deleted.fold(ErrorDeletingUser(s"User with Id($id) was not there").asLeft[Unit])(_ => Right(())))
      }
    }

}
```
This should be pretty straight-forward. We literally drop in `Future` for `F[_]` and fill in the implementation. 
For those who use intellij, the IDE basically gives you a basic skeleton, you just fill it in.

## Free
```scala
object DBFreeAlgebraT {
  /** previous code from above goes here **/
  /** previous code from above goes here **/
  /** previous code from above goes here **/
    val FutureInterpreter = new (DBFreeAlgebraT ~> Future) {
    /**
    * The above is equivalent to the signature below
    * `val FutureInterpreter = new FunctionK[DBFreeAlgebraT, Future]`
    * It's like a function on values from A => B
    * The only difference this works on "Kinds" hence `FunctionK`
    * This basically says you are creating a function from F[A] => F[B]
    * We're converting our free structure(F[A]) to a known Monad(F[B])
    * In this case, we're converting DBFreeAlgebra[T] => Future[T]
    */
        val users: mutable.Map[Long, User] = mutable.Map.empty
    
        override def apply[A](fa: DBFreeAlgebraT[A]): Future[A] =
          fa match {
            case Create(user) => //F[A]
              val castedUser = user.asInstanceOf[User]
              val inserted = users.put(castedUser.id, castedUser)
              Future.successful(inserted.isEmpty || inserted.isDefined).asInstanceOf[Future[A]] //F[B]
            case Read(id) => //F[A]
              Future.successful(users.get(id).toRight(ErrorFindingUser)).asInstanceOf[Future[A]] //F[B]
            case Delete(id) => { //F[A]
              import cats.syntax.either._
              val deleted = users.remove(id)
              Future.successful(
                deleted.fold(ErrorDeletingUser(s"User with Id($id) was not there").asLeft[Unit])(_ => Right(()))
              ).asInstanceOf[Future[A]] //F[B]
            }
          }
      }
}
```

The Free Monad is straight forward as well but a little more involved.
We have to do a little casting along the way because of Scala's limited GADT support(`.asInstanceOf[Future[A]]`). <- I may be wrong on this but the compiler errors above say otherwise
I'll explain `user.asInstanceOf[User]` a little later

Now that our code can actually do stuff, let's write some repos to wrap our low-level DB code:
## Tagless Final
```scala
class UserRepo[F[_]](DB: DatabaseAlgebra[F, User])(implicit M: Monad[F]) {

  def getUser(id: Long): F[Either[DatabaseError, User]] = DB.read(id)
  def addUser(user: User): F[Boolean] = DB.create(user)

  def updateUser(user: User): F[Either[DatabaseError, Boolean]] = {
    (for {
      userFromDB <- EitherT(getUser(user.id))
      successfullyAdded <- EitherT.liftF[F, DatabaseError, Boolean](addUser(user))
    } yield successfullyAdded).value
  }

}
```
## Free
```scala
class FreeUserRepo {
  val DB = DBFreeAlgebraT
  import DBFreeAlgebraT.DBFreeAlgebra

  def getUser(id: Long): DBFreeAlgebra[Either[DatabaseError, User]] = DB.read(id) //this is a program
  def addUser(user: User): DBFreeAlgebra[Boolean] = DB.create(user) //this is a program

//this is a program
  def updateUser(user: User): DBFreeAlgebra[Either[DatabaseError, Boolean]] = (for {
    userFromDB <- EitherT(getUser(user.id))
    successfullyAdded <- EitherT.liftF[DBFreeAlgebra, DatabaseError, Boolean](addUser(user))
  } yield successfullyAdded).value
}
```

The two repos look almost identical. 
Notice that the tagless final version accepts its interpreter as an argument while the Free version does not. 
The one big difference is the tagless final actually runs your code and returns your expected result.
The Free version simply builds a recursive structure. Something like `Suspend(Suspend(Pure(...))`. Don't quote me on that.
The structure gets broken down and traversed through a little later in the main method via the interpreter.

The code calling all of this:

## Tagless Final
```scala
object UserRepoRunner extends App {

  val repo = new UserRepo(DatabaseAlgebra.FutureInterpreter)

  println(Await.result(
    (for {
      _ <- repo.addUser(User(1, "Bob", 31))
      dbErrorOrSuccessfullyUpdated <- repo.updateUser(User(1, "Bobby", 31))
    } yield dbErrorOrSuccessfullyUpdated),
    1 second))

}
```

## Free
```scala
object DBFreeAlgebraRunner extends App {

  val repo = new FreeUserRepo
  
  println(Await.result(
    (for {
      _ <- repo.addUser(User(2, "Bob", 31))
      dbErrorOrSuccessfullyUpdated <- repo.updateUser(User(2, "Bobby", 31))
    } yield dbErrorOrSuccessfullyUpdated).foldMap(FutureInterpreter), //notice the foldMap(..) here
    1 second))

}
```

Output:
```bash
sbt:sample-blog-code> run

 [1] DBFreeAlgebraRunner
 [2] UserRepoRunner
[info] Packaging /Users/anthony.garo/git/sample-blog-code/target/scala-2.12/sample-blog-code_2.12-0.1.jar ...
[info] Done packaging.

Enter number: 1

[info] Running DBFreeAlgebraRunner
Right(true)

sbt:sample-blog-code> run
[warn] Multiple main classes detected.  Run 'show discoveredMainClasses' to see the list

Multiple main classes detected, select one to run:

 [1] DBFreeAlgebraRunner
 [2] UserRepoRunner
[info] Packaging /Users/anthony.garo/git/sample-blog-code/target/scala-2.12/sample-blog-code_2.12-0.1.jar ...
[info] Done packaging.

Enter number: 2

[info] Running UserRepoRunner
Right(true)
```

The main methods look almost identical.
They both include the FutureInterpreter somewhere.
In the Tagless Final version we just pass it into the repo.
In the Free version, we pass it in when we fold over our structure and map it to a Future.
In the Free version, our code STILL doesn't actually do anything until you call `.foldMap`.
The calls `repo.addUser(User(2, "Bob", 31))` and `repo.updateUser(User(2, "Bobby", 31))` simply build a recursive data structure.
This can can grow very large and that's fine because the cats implementation is stack safe. 
The `.foldMap` combinator folds over our structure every step at a time and converts that step to our known Monad using the `FutureInterpreter`.

And that's it!!!!


This alone can get you pretty far but at some point you're going to need to mix in another algebra into your program.

I'll play out the scenario where you want to log stuff.
This is very contrived but imagine it's something more sophisticated like sending an email.

Here's what an api/DSL for that would look like:

```scala
// vanilla api:
trait ConsoleAlgebra {
  def putLine[T](t: T): Unit
}

/** Tagless Final */
trait ConsoleAlgebra[F[_]] {
  def putLine[T](t: T): F[Unit]
}

/** Free */
sealed trait ConsoleFreeAlgebraT[T]
case class PutLine[T](t: T) extends ConsoleFreeAlgebraT[Unit]
```

Interpreters:
```scala
/** Tagless Final */
object ConsoleAlgebra {
  implicit object FutureInterpreter extends ConsoleAlgebra[Future] {
    override def putLine[T](t: T): Future[Unit] = Future.successful{
      println(t)
    }
  }
}

/** Free */
object ConsoleFreeAlgebraT {
  type ConsoleAlgebra[T] = Free[ConsoleFreeAlgebraT, T]

  def putLine[T](t: T): ConsoleFreeAlgebraT[Unit] = PutLine(t)

  val FutureInterpreter = new (ConsoleFreeAlgebraT ~> Future) {
    override def apply[A](fa: ConsoleFreeAlgebraT[A]): Future[A] =
      fa match {
        case PutLine(t) =>
          Future.successful(println(t)).asInstanceOf[Future[A]]
      }
  }
  
}
```

Now...the hard part. Combining the algebras:

## Tagless Final
```scala
class UserRepo[F[_]](DB: DatabaseAlgebra[F, User],
                     C: ConsoleAlgebra[F]) // only difference is here
                    (implicit M: Monad[F]) {

  def getUser(id: Long): F[Either[DatabaseError, User]] = DB.read(id)
  def addUser(user: User): F[Boolean] = DB.create(user)

  def updateUser(user: User): F[Either[DatabaseError, Boolean]] = {
    (for {
      userFromDB <- EitherT(getUser(user.id))
      _ <- EitherT.liftF(C.putLine(s"We found user($userFromDB)!!")) // this is new
      successfullyAdded <- EitherT.liftF[F, DatabaseError, Boolean](addUser(user))
    } yield successfullyAdded).value
  }

}
```

## Free
With Free, it's little more involved.
Since our algebras are multiple ADTs, we need to tell our code that any given line of code in our program
could be from our DB algebra OR from the console algebra.
Essentially, its' either one of these. This can be 2 or more algebras.
But before we can even get to that point we need to add a little boilerplate.
We have to make our distinct algebras have a common umbrella that they will fall under: `Free[_, _]`
That's what we have smart constructors for :-)
```scala
/** Step 1 - Wrap your smart constructors in a class with an implicit `InjectK` */
class DBFreeAlgebraTI[F[_]](implicit I: InjectK[DBFreeAlgebraT, F]) { 

    /** Step 2 - instead of calling `.liftF` you call `.inject` */
    def create[T](t: T): Free[F, Boolean] = // <- All algebras are `Free` now
      Free.inject[DBFreeAlgebraT, F](Create(t))

    def read[T](id: Long): Free[F, Either[DatabaseError, User]] = // <- All algebras are `Free` now
      Free.inject[DBFreeAlgebraT, F](Read(id))

    def delete[T](id: Long): Free[F, Either[DatabaseError, Unit]] = // <- All algebras are `Free` now
      Free.inject[DBFreeAlgebraT, F](Delete(id))
  }
  
  /** Step 3 - create an implicit instance of your new class */
  implicit def dBFreeAlgebraTI[F[_]](implicit I: InjectK[DBFreeAlgebraT, F]): DBFreeAlgebraTI[F] =
      new DBFreeAlgebraTI[F]
      
  /** Rinse and repeat for our console algebra */
  class ConsoleFreeAlgebraTI[F[_]](implicit I: InjectK[ConsoleFreeAlgebraT, F]) {
      def putLine[T](t: T): Free[F, Unit] = Free.inject[ConsoleFreeAlgebraT, F](PutLine(t)) // <- All algebras are `Free` now
  }
  
  implicit def consoleFreeAlgebraTI[F[_]](implicit I: InjectK[ConsoleFreeAlgebraT, F]): ConsoleFreeAlgebraTI[F] =
      new ConsoleFreeAlgebraTI[F]
```

I want to take a second to discuss InjectK. Just like FunctionK, InjectK works on Kinds.
So what is this doing exactly? I'll try to explain. Here goes...

Let's look at the type signature:
`abstract class InjectK[F[_], G[_]]`

InjectK takes your algebra `F[_]` and it injects it into some other algebra `G[_]`
So in our cases:
```scala
def create[T](t: T): Free[F, Boolean]
      Free.inject[DBFreeAlgebraT, F](Create(t))
```
Just zooming in: `Free.inject[DBFreeAlgebraT, F]`
We're injecting `DBFreeAlgebraT` into another algebra `F`.
In our case, `F` will be the combined algebra: `DbAndConsoleAlgebra` which you see below.

This [blog](https://underscore.io/blog/posts/2017/03/29/free-inject.html) summarizes it better than I do:
  1. implicitly resolving an instance of Inject[DBFreeAlgebraT, F]
  2. using it to inject `DBFreeAlgebraT` into `F[_]`, and finally
  3. lifting `F[_]` into the more generalized Free monad


Ok now that the boilerplate is out of the way,
we can tell our code that we have many algebras.
```scala
object Combined {
  type DbAndConsoleAlgebra[T] = EitherK[DBFreeAlgebraT, ConsoleFreeAlgebraT, T]

  val FutureInterpreter: DbAndConsoleAlgebra ~> Future =
    DBFreeAlgebraT.FutureInterpreter or ConsoleFreeAlgebraT.FutureInterpreter
}
```
EitherK works on Kinds like everything else with the 'K' suffix.
EitherK simply says it's either one container(with stuff in it) or another.
For us, this means our combined algebra is either `DBFreeAlgebraT` or `ConsoleFreeAlgebraT`.

So something like:
    SomeSpecificAlgebra(DB or Console) (inject)~> CombinedAlgebra(EitherK[_,_,_,...]) (liftF)~> Free(so everything is under 1 common umbrella)


Now we can finally get to the point where the tagless final version of the code is at above:
```scala
class FreeUserRepo(implicit
                   DB: DBFreeAlgebraT.DBFreeAlgebraTI[Combined.DbAndConsoleAlgebra],
                   C: ConsoleFreeAlgebraT.ConsoleFreeAlgebraTI[Combined.DbAndConsoleAlgebra]) {

  def getUser(id: Long): Free[Combined.DbAndConsoleAlgebra, Either[DatabaseError, User]] = DB.read(id)
  def addUser(user: User): Free[Combined.DbAndConsoleAlgebra, Boolean] = DB.create(user)

  /**
    * EitherT.liftF has the following signature:
    * def liftF[F[_], A, B](fb: F[B])
    *
    * We need this type alias because of the fact it only accepts an `F[_]`
    * and we're passing in `Free[DbAndConsoleAlgebra, A]`
    * The type alias fixes 1 parameter to fit the mold of `F[_]`
    */
  type DbAndConsoleAlgebraContainer[A] = Free[Combined.DbAndConsoleAlgebra, A]

  def updateUser(user: User): Free[Combined.DbAndConsoleAlgebra, Either[DatabaseError, Boolean]] = (for {
    userFromDB <- EitherT(getUser(user.id))
    _ <- EitherT.liftF(C.putLine(s"We found user($userFromDB)!!"))
    successfullyAdded <- EitherT.liftF[DbAndConsoleAlgebraContainer, DatabaseError, Boolean](addUser(user))
  } yield successfullyAdded).value

}
```

The main methods are almost identical:
```scala
/** Tagless Final */
object UserRepoRunner extends App {

  val repo = new UserRepo(DatabaseAlgebra.FutureInterpreter, ConsoleAlgebra.FutureInterpreter) // <- Console interpreter simply added in

  println(Await.result(
    (for {
      _ <- repo.addUser(User(1, "Bob", 31))
      dbErrorOrSuccessfullyUpdated <- repo.updateUser(User(1, "Bobby", 31))
    } yield dbErrorOrSuccessfullyUpdated),
    1 second))

}

/** Free */
object DBFreeAlgebraRunner extends App {

  val repo = new FreeUserRepo

  println(Await.result(
    (for {
      _ <- repo.addUser(User(1, "Bob", 31))
      dbErrorOrSuccessfullyUpdated <- repo.updateUser(User(1, "Bobby", 31))
    } yield dbErrorOrSuccessfullyUpdated).foldMap(Combined.FutureInterpreter), // <- New shiny interpreter
    1 second))

}
```

Output:
```bash
sbt:sample-blog-code> run
[warn] Multiple main classes detected.  Run 'show discoveredMainClasses' to see the list

Multiple main classes detected, select one to run:

 [1] DBFreeAlgebraRunner
 [2] UserRepoRunner

Enter number: 1

[info] Running DBFreeAlgebraRunner
We found user(User(1,Bob,31))!!
Right(true)

sbt:sample-blog-code> run
[warn] Multiple main classes detected.  Run 'show discoveredMainClasses' to see the list

Multiple main classes detected, select one to run:

 [1] DBFreeAlgebraRunner
 [2] UserRepoRunner

Enter number: 2

[info] Running UserRepoRunner
We found user(User(1,Bob,31))!!
Right(true)
```

THAT'S IT!!!!
WE DID IT!!!!

* I promised I'd mention `val castedUser = user.asInstanceOf[User]`
So, I have to come clean and confess something. I tried to be slick and make my DBAlgebra generic on one of it's types here:
`def create(t: T): Future[Boolean]`

Typically a DSL will not be this general and will specify the `T` to be user like so:
`def create(t: User): Future[Boolean]`

I went for it anyway to see if I could make it work and it turned out ok.
This was more proof to myself that you could use the techniques and attempt to maintain some level of generics.

I also wanted to challenge myself and demonstrate that you could write code like this and still use error handling patterns such as Either.
I feel like most examples, specifically around Free Monads, don't demonstrate how error handling can be used in your code.
I hope this can provide a basic example of a somewhat real life scenario around error handling with these techniques.

## What's the difference between the 2 approaches?
The most basic answer I can think of is tagless final expresses an api via functions
Free Monads express an api via ADTs

## Which should you choose?
Unfortunately this is where my lack of experience limits how I can answer this.
The biggest difference I can see is Free Monads allow you to inspect the ADTs that are constructed along the way.
This gives access you access to the entire call structure before it gets interpreted and the actions get executed.
I believe this makes room for further optimizations of your programs.

See below image:


The tagless final pattern lends itself to less code to achieve the ultimate end result.

## What's so cool about this stuff?
The main reason I was curious about the idea of abstracting over your monads was something I heard a few years ago at a Scala talk.
The speaker painted a scenario where the developer wanted to debug their code but it was asynchronous.
Using a pattern like this, the developer could write an integration test, create an interpreter usign the `Id` monad, and point their test
against a valid environment and watch the code execute synchronously and view the logs in an ordered fashion.

I thought this was a fascinating concept and was something I could relate to.

Ok that's enough from me.
Until the next time !
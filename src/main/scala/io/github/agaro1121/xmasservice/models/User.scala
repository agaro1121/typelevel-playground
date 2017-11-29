package io.github.agaro1121.xmasservice.models

/*
* TODO: id type?
* TODO: birthday type?
* TODO: ts type?
* */
case class User(id: String, firstName: String, lastName: String, birthday: String, ts: String)
object User {

  import cats.syntax.option.catsSyntaxOptionId

  val dummyUser = User(
    id = "someId",
    firstName = "Anthony",
    lastName = "Garo",
    birthday = "1988Nov21",
    ts = "someTimeStamp"
  )
  /*
  * Needed for parsing the POST route
  * */
  def unapply(str: String): Option[User] = dummyUser.some //TODO: convert to json then to object
}
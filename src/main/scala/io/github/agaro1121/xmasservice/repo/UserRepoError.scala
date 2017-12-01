package io.github.agaro1121.xmasservice.repo

sealed trait UserRepoError

case object UserNotFound extends UserRepoError
case class ConnectionError(msg: String) extends UserRepoError

object UserRepoError {
  def userNotFound: UserRepoError = UserNotFound
  def connectionError(msg: String): UserRepoError = ConnectionError(msg)
}
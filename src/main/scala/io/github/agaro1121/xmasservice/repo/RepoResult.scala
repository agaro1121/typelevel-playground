package io.github.agaro1121.xmasservice.repo

sealed trait RepoResult

object Success extends RepoResult
case class Failure(msg: String) extends RepoResult

object RepoResult {
  def success: RepoResult = Success
  def failure(msg: String): RepoResult = Failure(msg)
}
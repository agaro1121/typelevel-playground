name := "typelevel-playground"

version := "0.1"

scalaVersion := "2.12.4"


scalacOptions ++= Seq(
//  "-Xfatal-warnings", //doesn't compile if there's warnings
  "-Ypartial-unification"
)


libraryDependencies += "org.typelevel" %% "cats-core" % "1.6.0"
libraryDependencies += "org.typelevel" %% "cats-free" % "1.6.0"
val http4sVersion = "0.20.0"
libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,

  "org.http4s" %% "http4s-circe" % http4sVersion,
  "io.circe" %% "circe-generic" % "0.11.1",
  "io.circe" %% "circe-parser" % "0.11.1"
).map(d => d exclude("org.typelevel", "cats-core"))

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.25"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.6" % "test"

libraryDependencies += "io.monix" %% "monix" % "2.3.3"
libraryDependencies += "io.monix" %% "monix-cats" % "2.3.3"
libraryDependencies += "org.typelevel" %% "cats-effect" % "1.2.0"

name := "typelevel-playground"

version := "0.1"

scalaVersion := "2.12.4"


scalacOptions ++= Seq(
//  "-Xfatal-warnings", //doesn't compile if there's warnings
  "-Ypartial-unification"
)


libraryDependencies += "org.typelevel" %% "cats-core" % "1.0.1"
libraryDependencies += "org.typelevel" %% "cats-free" % "1.0.1"
libraryDependencies += "org.typelevel" %% "cats-macros" % "1.0.1"
val http4sVersion = "0.18.0-M5"
libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,

  "org.http4s" %% "http4s-circe" % http4sVersion,
  "io.circe" %% "circe-generic" % "0.9.0-M2",
  "io.circe" %% "circe-parser" % "0.9.0-M2"
).map(d => d exclude("org.typelevel", "cats-core"))

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.25"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % "test"

libraryDependencies += "io.monix" %% "monix" % "2.3.0"
libraryDependencies += "io.monix" %% "monix-cats" % "2.3.0"
libraryDependencies += "org.typelevel" %% "cats-effect" % "0.5"

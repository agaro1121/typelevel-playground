name := "typelevel-playground"

version := "0.1"

scalaVersion := "2.12.4"


scalacOptions ++= Seq(
//  "-Xfatal-warnings", //doesn't compile if there's warnings
  "-Ypartial-unification"
)


libraryDependencies += "org.typelevel" %% "cats-core" % "1.0.0-RC1"
val http4sVersion = "0.18.0-M5"
libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,

  "org.http4s" %% "http4s-circe" % http4sVersion,
  "io.circe" %% "circe-generic" % "0.9.0-M2"
).map(d => d exclude("org.typelevel", "cats-core"))

name := """firesocks-core"""

version := "0.1"

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-deprecation")

resolvers ++= Seq(
  Resolver.sonatypeRepo("public"),
  Resolver.sonatypeRepo("releases")
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",

  "com.github.scopt" %% "scopt" % "3.3.0",

  "com.typesafe.akka" %% "akka-actor" % "2.3.14"
)

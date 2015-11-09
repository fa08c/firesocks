name := """firesocks"""

version := "0.1"

scalaVersion := "2.11.7"

// Change this to another test framework if you prefer
libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

// Uncomment to use Akka
//libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.11"

lazy val root = (project in file("."))
  .dependsOn(core, proxy, web)
  .aggregate(core, proxy, web)

lazy val core = project in file("modules/core")

lazy val proxy = (project in file("modules/proxy"))
  .dependsOn(core)

lazy val web = (project in file("modules/web"))
  .dependsOn(core)
  .enablePlugins(PlayScala)

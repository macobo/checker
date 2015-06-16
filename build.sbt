name := "Checker"

version := "1.0"

scalaVersion := "2.11.6"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Ywarn-unused",
  "-Ywarn-unused-import"
)

// Akka ecosystem
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.11",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.11"
)

// Library for test scaffolding.
libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"

// :TODO: needed for the naive way we're mixing in the disque client.
libraryDependencies += "net.debasishg" %% "redisclient" % "3.0"

// Library for parsing json.
libraryDependencies += "org.json4s" %% "json4s-native" % "3.2.11"

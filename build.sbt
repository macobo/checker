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

// Libraries for logging.
//libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.3.9"

// Library for test scaffolding.
libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"

// :TODO: needed for the naive way we're mixing in the disque client.
libraryDependencies += "net.debasishg" %% "redisclient" % "3.0"

// Library for parsing json.
libraryDependencies += "io.spray" %%  "spray-json" % "1.3.2"

// Datetime manipulation (jodatime scala wrapper)
libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.0.0"

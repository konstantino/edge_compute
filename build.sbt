val akkaVersion = "2.3.0"

name := "cluster"

version := "1.0"

scalaVersion := "2.10.4"

scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
      "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion,
      "com.typesafe.akka" %% "akka-remote" % "2.3.2",
      "com.github.nscala-time" %% "nscala-time" % "1.2.0",
      "org.scalatest" %% "scalatest" % "2.0" % "test",
      "org.fusesource" % "sigar" % "1.6.4")


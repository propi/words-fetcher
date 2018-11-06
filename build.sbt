enablePlugins(PackPlugin)

name := "words-fetcher"

version := "1.0"

scalaVersion := "2.12.2"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.14"
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.5.14"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.1.3"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.3"
libraryDependencies += "com.github.kxbmap" %% "configs" % "0.4.4"
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.7"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.7.1"

packMain := Map("main" -> "com.github.propi.wordsfetcher.MainWeb")
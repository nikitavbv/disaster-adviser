name := "disaster-adviser-v2"

version := "0.1"

scalaVersion := "2.13.7"

idePackagePrefix := Some("com.nikitavbv.disaster")

// akka
val AkkaVersion = "2.6.18"
val AkkaHttpVersion = "10.2.7"

libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % AkkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http-xml" % AkkaHttpVersion
name := "akka-zio"

version := "0.1"

scalaVersion := "2.13.3"

libraryDependencies ++= Seq(
  "ch.qos.logback"     % "logback-classic"   % "1.2.3",
  "com.typesafe.akka" %% "akka-http"         % "10.2.0",
  "com.typesafe.akka" %% "akka-stream-typed" % "2.6.8",
  "dev.zio"           %% "zio"               % "1.0.0"
)

enablePlugins(JavaAppPackaging)
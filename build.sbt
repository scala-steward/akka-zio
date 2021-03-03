name := "akka-zio"

version := "0.1"

scalaVersion := "2.13.5"

libraryDependencies ++= Seq(
  "ch.qos.logback"     % "logback-classic"   % "1.2.3",
  "com.typesafe.akka" %% "akka-http"         % "10.2.4",
  "com.typesafe.akka" %% "akka-stream-typed" % "2.6.13",
  "dev.zio"           %% "zio"               % "1.0.4-2"
)

enablePlugins(JavaAppPackaging)

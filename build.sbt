name := "akka-zio"

version := "0.1"

scalaVersion := "2.13.4"

libraryDependencies ++= Seq(
  "ch.qos.logback"     % "logback-classic"   % "1.2.3",
  "com.typesafe.akka" %% "akka-http"         % "10.2.2",
  "com.typesafe.akka" %% "akka-stream-typed" % "2.6.13",
  "dev.zio"           %% "zio"               % "1.0.3"
)

enablePlugins(JavaAppPackaging)
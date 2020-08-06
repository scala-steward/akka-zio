package io.github.mvillafuertem

import akka.actor.BootstrapSetup
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.{ actor, Done }
import io.github.mvillafuertem.AkkaZioApplication.platform
import zio._
import zio.console.Console

object AkkaZioConfiguration {

  lazy val program: URIO[Any with Console, ExitCode] = ZManaged
    .fromEffect(
      for {
        actorSystem <- actorSystem
        server      <- httpServer(actorSystem, route)
      } yield server
    )
    .useForever
    .exitCode

  private lazy val route: Route =
    path("hello") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
      }
    }

  private lazy val actorSystem: ZIO[Any, Throwable, ActorSystem[Done]] =
    for {
      configurationProperties <- Task(AkkaZioConfigurationProperties())
      executionContext                <- Task(platform.executor.asEC)
      actorSystem                     <- Task(
                                           ActorSystem[Done](
                                             Behaviors.setup[Done] { context =>
                                               context.setLoggerName(this.getClass)
                                               context.log.info(s"Starting ${configurationProperties.name}... ${"BuildInfo.toJson"}")
                                               Behaviors.receiveMessage {
                                                 case Done =>
                                                   context.log.error(s"Server could not start!")
                                                   Behaviors.stopped
                                               }
                                             },
                                             configurationProperties.name.toLowerCase(),
                                             BootstrapSetup().withDefaultExecutionContext(executionContext)
                                           )
                                         )
    } yield actorSystem

  private def httpServer(actorSystem: ActorSystem[Done], route: Route): ZIO[Any, Throwable, Http.ServerBinding] =
    for {
      configurationProperties <- Task(AkkaZioConfigurationProperties())

      eventualBinding <- Task {
                           implicit lazy val untypedSystem: actor.ActorSystem = actorSystem.toClassic
                           implicit lazy val materializer: Materializer       = Materializer(actorSystem)
                           Http().bindAndHandle(
                             route,
                             configurationProperties.interface,
                             configurationProperties.port
                           )
                         }
      server          <- Task
                           .fromFuture(_ => eventualBinding)
                           .tapError(exception =>
                             UIO(
                               actorSystem.log.error(
                                 s"Server could not start with parameters [host:port]=[${configurationProperties.interface},${configurationProperties.port}]",
                                 exception
                               )
                             )
                           )
                           .tap(_ =>
                             ZIO.effect(
                               actorSystem.log.info(
                                 s"Server online at http://${configurationProperties.interface}:${configurationProperties.port}/"
                               )
                             )
                           )
    } yield server

}

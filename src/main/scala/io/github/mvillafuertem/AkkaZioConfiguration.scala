package io.github.mvillafuertem

import akka.actor.BootstrapSetup
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.{ actor, Done }
import io.github.mvillafuertem.AkkaZioApplication.platform
import zio._
import zio.console.Console

import scala.concurrent.ExecutionContext

object AkkaZioConfiguration {

  type ZActorSystem             = Has[ActorSystem[Done]]
  type ZExecutionContext        = Has[ExecutionContext]
  type ZConfigurationProperties = Has[AkkaZioConfigurationProperties]

  private val configurationPropertiesLayer: TaskLayer[ZConfigurationProperties] =
    ZLayer.fromEffect(Task.effect(AkkaZioConfigurationProperties()))

  private val akkaZioEnv: TaskLayer[ZActorSystem with ZConfigurationProperties] =
    ZLayer.fromEffect(Task.effect(platform.executor.asEC)) ++
      configurationPropertiesLayer >>>
      ZLayer.fromEffect(actorSystem) ++ configurationPropertiesLayer

  lazy val program: URIO[Console, ExitCode] = ZManaged
    .fromEffect(httpServer(AkkaZioApi.route))
    .useForever
    .provideLayer(akkaZioEnv)
    .exitCode

  lazy val actorSystem: RIO[ZExecutionContext with ZConfigurationProperties, ActorSystem[Done]] =
    for {
      executionContext        <- ZIO.access[ZExecutionContext](_.get)
      configurationProperties <- ZIO.access[ZConfigurationProperties](_.get)
      actorSystem             <- Task.effect(
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

  private def httpServer(route: Route): RIO[ZActorSystem with ZConfigurationProperties, Http.ServerBinding] =
    for {
      actorSystem             <- ZIO.access[ZActorSystem](_.get)
      configurationProperties <- ZIO.access[ZConfigurationProperties](_.get)
      eventualBinding         <- Task.effect {
                                   implicit lazy val untypedSystem: actor.ActorSystem = actorSystem.toClassic
                                   implicit lazy val materializer: Materializer       = Materializer(actorSystem)
                                   Http().newServerAt(configurationProperties.interface, configurationProperties.port).bind(route)
                                 }
      server                  <- Task
                                   .fromFuture(_ => eventualBinding)
                                   .tapError(exception =>
                                     Task.effect(
                                       actorSystem.log.error(
                                         s"Server could not start with parameters [host:port]=[${configurationProperties.interface},${configurationProperties.port}]",
                                         exception
                                       )
                                     )
                                   )
                                   .tap(_ =>
                                     Task.effect(
                                       actorSystem.log.info(
                                         s"Server online at http://${configurationProperties.interface}:${configurationProperties.port}/"
                                       )
                                     )
                                   )
    } yield server

}

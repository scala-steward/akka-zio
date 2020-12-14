package io.github.mvillafuertem

import akka.actor.BootstrapSetup
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.stream.Materializer
import akka.{ actor, Done }
import zio.console.Console
import zio.{ ZEnv, _ }

object AkkaZioConfiguration {

  type ZActorSystem             = Has[ActorSystem[Done]]
  type ZConfigurationProperties = Has[AkkaZioConfigurationProperties]
  type ZApi                     = Has[AkkaZioApi]
  type ZAkkaZio                 = ZActorSystem with ZConfigurationProperties with ZApi

  private val akkaZioEnv: ZLayer[Any, Throwable, zio.ZEnv with ZAkkaZio] =
    ZEnv.live >+>
      ZLayer.fromEffect(Task.effect(AkkaZioConfigurationProperties())) >+>
      ZLayer.fromManaged(actorSystem) >+>
      ZLayer.fromEffect(Task.effect(AkkaZioApi()))

  lazy val program: URIO[zio.ZEnv, ExitCode] = ZManaged
    .fromEffect(httpServer)
    .useForever
    .provideLayer(akkaZioEnv)
    .exitCode

  lazy val actorSystem: ZManaged[Any with Console with ZConfigurationProperties with ZEnv, Throwable, ActorSystem[Done]] =
    ZManaged.make(for {
      runtime                 <- ZIO.runtime[ZEnv]
      configurationProperties <- ZIO.access[ZConfigurationProperties](_.get)
      actorSystem             <- Task.effect(
                                   ActorSystem[Done](
                                     Behaviors.setup[Done] { context =>
                                       context.setLoggerName(this.getClass)
                                       context.log.info(s"Starting ${configurationProperties.name}...")
                                       Behaviors.receiveMessage {
                                         case Done =>
                                           context.log.error(s"Server could not start!")
                                           Behaviors.stopped
                                       }
                                     },
                                     configurationProperties.name.toLowerCase(),
                                     BootstrapSetup().withDefaultExecutionContext(runtime.platform.executor.asEC)
                                   )
                                 )
    } yield actorSystem)(actorSystem => Task.effect(actorSystem.terminate()).exitCode)

  private lazy val httpServer: RIO[ZAkkaZio, Http.ServerBinding] =
    for {
      actorSystem             <- ZIO.access[ZActorSystem](_.get)
      configurationProperties <- ZIO.access[ZConfigurationProperties](_.get)
      route                   <- ZIO.access[ZApi](_.get.route)
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

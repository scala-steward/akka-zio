package io.github.mvillafuertem

import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.http.scaladsl.server.Directives.{ complete, get, path }
import akka.http.scaladsl.server.Route

final class AkkaZioApi {

  lazy val route: Route =
    path("hello") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-zio</h1>"))
      }
    }

}

object AkkaZioApi {

  def apply(): AkkaZioApi = new AkkaZioApi()

}

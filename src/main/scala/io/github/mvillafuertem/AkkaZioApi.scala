package io.github.mvillafuertem

import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, StatusCodes }
import akka.http.scaladsl.server.Directives.{ complete, get, path, pathEndOrSingleSlash, redirect }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RouteConcatenation._

final class AkkaZioApi {

  lazy val route: Route =
    pathEndOrSingleSlash {
      redirect("/hello", StatusCodes.PermanentRedirect)
    } ~ path("hello") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-zio</h1>"))
      }
    }
}

object AkkaZioApi {

  def apply(): AkkaZioApi = new AkkaZioApi()

}

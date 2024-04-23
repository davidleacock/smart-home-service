package http

import cats.effect.{ExitCode, IO, Resource}
import com.comcast.ip4s._
import org.http4s.Method.GET
import org.http4s.{HttpRoutes, _}
import org.http4s.dsl.impl.UUIDVar
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.{Router, Server}
import service.SmartHomeService
import service.SmartHomeService.GetSmartHome

// ! Make this an object that returns the server?
class HttpServer(smartHomeService: SmartHomeService[IO]) {

  // ! TODO - test this
  // ! TODO - add more routes
  // ! TODO - handle errors
  private val routes = HttpRoutes.of[IO] { case GET -> Root / "home" / UUIDVar(homeId) =>
    Ok(smartHomeService.processCommand(homeId, GetSmartHome).map(result => s"Home $homeId - $result"))
  }

  private val httpApp: HttpApp[IO] = Router("/" -> routes).orNotFound

  // Is this what I want to return from the HttpServer?
  val server: Resource[IO, Server] =
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(httpApp)
      .build

  server
    .use { server =>
      println(s"Server up and running ${server.address}")
      IO.never // <- non terminating IO
    }
    .as(ExitCode.Success)
}

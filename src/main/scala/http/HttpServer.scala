package http

import cats.effect.{ExitCode, IO, Resource}
import com.comcast.ip4s._
import org.http4s.Method.GET
import org.http4s.{HttpRoutes, _}
import org.http4s.dsl.impl.UUIDVar
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger
import org.http4s.server.{Router, Server}
import service.SmartHomeService
import service.SmartHomeService.{Failure, GetSmartHome, ResponseResult, Success}

// ! TODO - test this
class HttpServer(serverResource: Resource[IO, Server]) {

  // ! TODO - handle errors
  def start: IO[Unit] =
    serverResource
      .use { server =>
        IO(println(s"Server up and running ${server.address}")) *> IO.never
      }
      .as(ExitCode.Success)
}

object HttpServer {

  // ! TODO - Add more commands/routes, break down the failures and responses even further
  def createServerResources(smartHomeService: SmartHomeService[IO]): Resource[IO, Server] = {
    val routes = HttpRoutes.of[IO] { case GET -> Root / "home" / UUIDVar(homeId) =>
      smartHomeService
        .processCommand(homeId, GetSmartHome)
        .flatMap {
          case Success => Ok("SmartHome command Success")
          case ResponseResult(payload) => Ok(s"SmartHome response: $payload")
          case Failure(reason) => BadRequest(s"Unable to retrieve SmartHome reason:$reason")
        }
    }

    val httpApp: HttpApp[IO] = Logger.httpApp(logHeaders = true, logBody = true) {
      Router("/" -> routes).orNotFound
    }

    EmberServerBuilder
          .default[IO]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(httpApp)
          .build
  }
}

package http

import cats.effect.{IO, Resource}
import com.comcast.ip4s._
import org.http4s.Method.GET
import org.http4s.dsl.impl.UUIDVar
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger
import org.http4s.server.{Router, Server}
import org.http4s.{HttpRoutes, _}
import service.SmartHomeService
import service.SmartHomeService.{Failure, GetSmartHome, ResponseResult, Success}

object SmartHomeHttpServer {
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

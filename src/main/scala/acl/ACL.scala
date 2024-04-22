package acl

import domain.{DeviceValueType, IntDVT, MotionDetector, StringDVT, Thermostat}
import io.circe.generic.semiauto._
import io.circe.{Decoder, parser}
import service.SmartHomeService
import service.SmartHomeService.{AddDevice, UpdateDevice}
import io.circe._
import io.circe.parser._
import io.circe.syntax._

import java.util.UUID

// Anti-corruption layer to convert external events into domain commands
object ACL {

  case class ExternalMessage(cmdType: String, payload: DevicePayload)

  sealed trait DeviceValue
  private case class IntValue(value: Int) extends DeviceValue
  private case class StringValue(value: String) extends DeviceValue

  sealed trait DevicePayload
  case class AddDevicePayload(deviceType: String, deviceId: UUID, initialValue: DeviceValue) extends DevicePayload
  case class UpdateDevicePayload(deviceId: UUID, newValue: DeviceValue) extends DevicePayload

  implicit val decodeExternalMessage: Decoder[ExternalMessage] = deriveDecoder
  implicit val decodeAddDevicePayloadMessage: Decoder[AddDevicePayload] = deriveDecoder
  implicit val decodeUpdateDevicePayloadMessage: Decoder[UpdateDevicePayload] = deriveDecoder

  implicit val decodeDeviceValue: Decoder[DeviceValue] = Decoder.instance { cursor =>
    cursor.get[String]("type").flatMap {
      case "int" => cursor.get[Int]("value").map(IntValue)
      case "string" => cursor.get[String]("value").map(StringValue)
      case _ => Left(DecodingFailure("Unsupported type", cursor.history))
    }
  }

  implicit val decodeDevicePayload: Decoder[DevicePayload] = Decoder.instance { cursor =>
    cursor.downField("deviceType").as[String].toOption match {
      case Some(_) => decodeAddDevicePayloadMessage(cursor)
      case None => decodeUpdateDevicePayloadMessage(cursor)
    }
  }

  // ! TODO Replace exception with custom errors
  def parseEvent(event: String): Either[Throwable, SmartHomeService.Command] =
    parser.decode[ExternalMessage](event).flatMap { msg =>
      msg.cmdType match {

        case "AddDevice" => msg.payload match {
          case AddDevicePayload(deviceType, deviceId, IntValue(initialValue)) if deviceType == "Thermostat" =>
            Right(AddDevice(Thermostat(deviceId, initialValue)))
          case AddDevicePayload(deviceType, deviceId, StringValue(initialValue)) if deviceType == "MotionDetector" =>
            Right(AddDevice(MotionDetector(deviceId, initialValue)))

          case _ => Left(new Exception("Invalid payload for AddDevice"))
        }

        case "UpdateDevice" => msg.payload match {
          case UpdateDevicePayload(deviceId, IntValue(updatedValue)) =>
            Right(UpdateDevice(deviceId, IntDVT(updatedValue)))
          case UpdateDevicePayload(deviceId, StringValue(updatedValue)) =>
            Right(UpdateDevice(deviceId, StringDVT(updatedValue)))
          case _ => Left(new Exception("Invalid payload for UpdateDevice"))
        }


        // ! This technical may not be an error, but rather we're just not interested in this data. This will get
        // ! changed after I write the custom reply that i'll flesh out during testing
        case _ => Left(new Exception("Unsupported command"))
      }
    }
}

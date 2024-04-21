package acl

import domain.{Device, DeviceValueType}
import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Decoder, parser}
import service.SmartHomeService
import service.SmartHomeService.{AddDevice, UpdateDevice}
import utils.EncoderDecoder

import java.util.UUID

// Anti-corruption layer to convert external events into domain commands
object ACL {

  case class ExternalMessage(cmdType: String, payload: Option[MessagePayload])
  case class MessagePayload(homeId: UUID, deviceId: Option[UUID], newValue: Option[DeviceValueType])
  case class DevicePayload(homeId: UUID, deviceId: Option[UUID], newValue: Option[DeviceValueType])

  implicit val deviceDecoder: Decoder[Device] = EncoderDecoder.decoderDevice
  implicit val decodeDeviceValueType: Decoder[DeviceValueType] = EncoderDecoder.decodeDeviceValueType
  implicit val messagePayloadDecoder: Decoder[MessagePayload] = deriveDecoder
  implicit val externalMessageDecoder: Decoder[ExternalMessage] = deriveDecoder

  // ! TODO Replace exception with custom errors
  def parseEvent(event: String): Either[Throwable, SmartHomeService.Command] =
    parser.decode[ExternalMessage](event).flatMap { msg =>
      msg.cmdType match {

        case "UpdateDevice" =>
          for {
            payload <- msg.payload.toRight(new Exception("payload missing for UpdateDevice"))
            deviceId <- payload.deviceId.toRight(new Exception("deviceId missing for UpdateDevice"))
            newValue <- payload.newValue.toRight(new Exception("newValue missing for UpdateDevice"))
          } yield UpdateDevice(deviceId, newValue)

        // ! TODO Finish ACL
        case "AddDevice" => {
          for {
            payload <- msg.payload.toRight(new Exception("payload missing for AddDevice"))
            device <- payload.device.toRight("")

          } yield AddDevice()
        }

        // ! This technical may not be an error, but rather we're just not interested in this data. This will get
        // ! changed after I write the custom reply that i'll flesh out during testing
        case _ => Left(new Exception("Unsupported command"))
      }
    }
}

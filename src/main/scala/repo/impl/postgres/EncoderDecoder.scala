package repo.impl.postgres

import domain._
import doobie.util.meta.Meta
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}
import org.postgresql.util.PGobject
import service.SmartHomeService.{DeviceAdded, DeviceUpdated, Event}

// TODO move this, should it live inside its class or maybe it makes more sense just to keep here?
// TODO what all is still needed?
object EncoderDecoder {

  implicit val encodeDeviceValueType: Encoder[DeviceValueType] = Encoder.instance {
    case IntDVT(value)    => Json.obj("type" -> Json.fromString("IntDVT"), "value" -> Json.fromInt(value))
    case StringDVT(value) => Json.obj("type" -> Json.fromString("StringDVT"), "value" -> Json.fromString(value))
  }

  implicit val decodeDeviceValueType: Decoder[DeviceValueType] = Decoder.instance { cursor =>
    cursor.get[String]("type").flatMap {
      case "IntDVT"    => cursor.get[Int]("value").map(IntDVT)
      case "StringDVT" => cursor.get[String]("value").map(StringDVT)
    }
  }

  implicit val encodeDevice: Encoder[Device] = Encoder.instance {
    case thermostat: Thermostat         => thermostat.asJson
    case motionDetector: MotionDetector => motionDetector.asJson
  }

  implicit val decoderDevice: Decoder[Device] = Decoder.instance { cursor =>
    cursor.get[String]("deviceType").flatMap {
      case "Thermostat"     => cursor.as[Thermostat]
      case "MotionDetector" => cursor.as[MotionDetector]
    }
  }

  implicit val thermostatEncoder: Encoder[Thermostat] = deriveEncoder
  implicit val thermostatDecoder: Decoder[Thermostat] = deriveDecoder

  implicit val motionDetectorEncoder: Encoder[MotionDetector] = deriveEncoder
  implicit val motionDetectorDecoder: Decoder[MotionDetector] = deriveDecoder

  implicit val eventMeta: Meta[Event] = Meta
    .Advanced
    .other[PGobject]("jsonb")
    .timap[Event](pgObject => decode[Event](pgObject.getValue).getOrElse(throw new Exception("decoder error")) // can I replace this with an error type?
    ) { event =>
      val pgObject = new PGobject()
      pgObject.setType("jsonb")
      pgObject.setValue(event.asJson.noSpaces)
      pgObject
    }

  implicit val encodeEvent: Encoder[Event] = Encoder.instance {
    case DeviceAdded(device)   => Json.obj("eventType" -> Json.fromString("DeviceAdded"), "device" -> device.asJson)
    case DeviceUpdated(device) => Json.obj("eventType" -> Json.fromString("DeviceUpdated"), "device" -> device.asJson)
  }

  implicit val decodeEvent: Decoder[Event] = Decoder.instance { cursor =>
    cursor.get[String]("eventType").flatMap {
      case "DeviceAdded"   => cursor.get[Device]("device").map(DeviceAdded)
      case "DeviceUpdated" => cursor.get[Device]("device").map(DeviceUpdated)
    }
  }
}
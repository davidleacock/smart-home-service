package utils

import domain._
import doobie.util.meta.Meta
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import service.SmartHomeService.{DeviceAdded, DeviceUpdated, Event, TemperatureSettingsSet}

object EncoderDecoder {

  implicit val eventMeta: Meta[Event] =
    Meta[String]
      .timap[Event] { pgObject =>
        decode[Event](pgObject) match {
          case Right(event) => event
          case Left(error)  =>
            // TODO make this more robust?
            println(s"decoding error $error")
            throw error
        }
      } { event =>
        event.asJson.noSpaces
      }

  /* Encoders */
  implicit val thermostatEncoder: Encoder[Thermostat] = deriveEncoder
  implicit val motionDetectorEncoder: Encoder[MotionDetector] = deriveEncoder
  implicit val temperatureSettingsEncoder: Encoder[TemperatureSettings] = deriveEncoder
  implicit val intDVTEncoder: Encoder[IntDVT] = deriveEncoder


  implicit val encodeDeviceValueType: Encoder[DeviceValueType] = Encoder.instance {
    case IntDVT(value)    => Json.obj("type" -> Json.fromString("IntDVT"), "value" -> Json.fromInt(value))
    case StringDVT(value) => Json.obj("type" -> Json.fromString("StringDVT"), "value" -> Json.fromString(value))
  }

  implicit val encodeDevice: Encoder[Device] = Encoder.instance {
    case thermostat: Thermostat =>
      thermostat
        .asJson
        .mapObject(_.add("deviceType", Json.fromString("Thermostat")))
        .mapObject(_.add("type", Json.fromString("IntDVT")))
    case motionDetector: MotionDetector =>
      motionDetector
        .asJson
        .mapObject(_.add("deviceType", Json.fromString("MotionDetector")))
        .mapObject(_.add("type", Json.fromString("StringDVT")))
  }

  implicit val encodeEvent: Encoder[Event] = Encoder.instance {
    case DeviceAdded(device)   => Json.obj("eventType" -> Json.fromString("DeviceAdded"), "device" -> device.asJson)
    case DeviceUpdated(device) => Json.obj("eventType" -> Json.fromString("DeviceUpdated"), "device" -> device.asJson)
    case TemperatureSettingsSet(settings) =>
      Json.obj("eventType" -> Json.fromString("TemperatureSettingsSet"), "settings" -> settings.asJson)
  }

  /* Decoders   */
  implicit val thermostatDecoder: Decoder[Thermostat] = deriveDecoder
  implicit val motionDetectorDecoder: Decoder[MotionDetector] = deriveDecoder
  implicit val temperatureSettingsDecoder: Decoder[TemperatureSettings] = deriveDecoder

  implicit val decodeDeviceValueType: Decoder[DeviceValueType] = Decoder.instance { cursor =>
    cursor.get[String]("type").flatMap {
      case "IntDVT"    => cursor.get[Int]("value").map(IntDVT)
      case "StringDVT" => cursor.get[String]("value").map(StringDVT)
    }
  }

  implicit val decoderDevice: Decoder[Device] = Decoder.instance { cursor =>
    cursor.get[String]("deviceType").flatMap {
      case "Thermostat"     => cursor.as[Thermostat]
      case "MotionDetector" => cursor.as[MotionDetector]
      case other            => Left(DecodingFailure(s"Unknown deviceType: $other", cursor.history))
    }
  }

  implicit val decodeEvent: Decoder[Event] = Decoder.instance { cursor =>
    cursor.get[String]("eventType").flatMap {
      case "DeviceAdded"            => cursor.get[Device]("device").map(DeviceAdded)
      case "DeviceUpdated"          => cursor.get[Device]("device").map(DeviceUpdated)
      case "TemperatureSettingsSet" => cursor.get[TemperatureSettings]("settings").map(TemperatureSettingsSet)
    }
  }
}

package service

import domain.{Device, DeviceValueType}
import service.SmartHomeService.{Command, SmartHomeResult}

import java.util.UUID

trait SmartHomeService[F[_]] {
  def processCommand(homeId: UUID, command: Command): F[SmartHomeResult]
}

object SmartHomeService {
  sealed trait Command
  case class AddDevice(device: Device) extends Command
  case class UpdateDevice(deviceId: UUID, newValue: DeviceValueType) extends Command
  case object GetSmartHome extends Command

  sealed trait Event
  case class DeviceAdded(device: Device) extends Event
  case class DeviceUpdated(device: Device) extends Event

  sealed trait SmartHomeResult
  case object Success extends SmartHomeResult
  case class ResponseResult(payload: String) extends SmartHomeResult
  case class Failure(reason: String) extends SmartHomeResult
}
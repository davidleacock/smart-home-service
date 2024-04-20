package service

import domain.{Device, DeviceValueType, TemperatureSettings}
import service.SmartHomeService.{Command, SmartHomeResult}

import java.util.UUID

trait SmartHomeService[F[_]] {
  def processCommand(homeId: UUID, command: Command): F[SmartHomeResult]
}

object SmartHomeService {
  sealed trait Command
  // SmartHome commands
  case object GetSmartHome extends Command
  case class SetTemperatureSettings(minimumTemperature: Int, maximumTemperature: Int) extends Command

  sealed trait CommandResult
  case class EventSuccess(event: Event) extends CommandResult
  case class CommandResponse(payload: String) extends CommandResult
  case class CommandFailed(reason: String) extends CommandResult

  // Device commands
  case class AddDevice(device: Device) extends Command
  case class UpdateDevice(deviceId: UUID, newValue: DeviceValueType) extends Command

  sealed trait Event
  case class DeviceAdded(device: Device) extends Event
  case class DeviceUpdated(device: Device) extends Event
  case class TemperatureSettingsSet(temperatureSettings: TemperatureSettings) extends Event

  sealed trait SmartHomeResult
  case object Success extends SmartHomeResult
  case class ResponseResult(payload: String) extends SmartHomeResult
  case class Failure(reason: String) extends SmartHomeResult
}
package rules

import cats.data.{Validated, ValidatedNec}
import domain.{SmartHome, TemperatureSettings}
import service.SmartHomeService._

class SmartHomeRuleEngine extends RuleEngine[Command, SmartHome, CommandResult] {
  override def validate(command: Command, state: SmartHome): ValidatedNec[ValidationError, CommandResult] =
    command match {
      case AddDevice(device) =>
        Validated.valid(EventSuccess(DeviceAdded(device)))
      case UpdateDevice(deviceId, newValue) =>
        state.devices.find(_.id == deviceId) match {
          case Some(device) =>
            device.applyUpdate(newValue) match {
              case Right(updatedDevice) => Validated.valid(EventSuccess(DeviceUpdated(updatedDevice)))
              case Left(error)          => Validated.invalidNec(error.reason)
            }
          case None => Validated.invalidNec(s"Device $deviceId not found.")
        }
      case SetTemperatureSettings(min, max) =>
        if (min < max && min >= 0 && max <= 50) // Arbitrary range constraints for example
          Validated.valid(EventSuccess(TemperatureSettingsSet(TemperatureSettings(min, max))))
        else
          Validated.invalidNec(s"Invalid temperature settings: [$min, $max] must be between 0 and 50.")
      case GetSmartHome =>
        // TODO improve this response
        Validated.valid(
          CommandResponse(s"Result from ${state.homeId}: ${state.devices} currentTemp: ${state.currentTemperature} motion: ${state.motionState}")
        )

    }
}

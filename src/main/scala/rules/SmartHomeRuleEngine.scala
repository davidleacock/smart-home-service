package rules

import cats.data.{Validated, ValidatedNec}
import domain.{SmartHome, TemperatureSettings}
import service.SmartHomeService._

class SmartHomeRuleEngine extends RuleEngine[Command, SmartHome, CommandResult] {

  // TODO I want to define this in a config so it can be injected in
  private val minimumTemp = 0
  private val maximumTemp = 50

  override def validate(command: Command, state: SmartHome): ValidatedNec[ValidationError, CommandResult] =
    command match {
      case AddDevice(device) =>
        Validated.valid(EventSuccess(DeviceAdded(device)))
      case UpdateDevice(deviceId, newValue) =>
        state.devices.find(_.id == deviceId) match {
          case Some(device) =>
            device
              .applyUpdate(newValue)
              .leftMap(deviceErrors => deviceErrors.map(e => e.reason))
              .andThen(updatedDevice => Validated.valid(EventSuccess(DeviceUpdated(updatedDevice))))
          case None => Validated.invalidNec(s"Device $deviceId not found.")
        }
      case SetTemperatureSettings(min, max) =>
        if (min < max && min >= minimumTemp && max <= maximumTemp)
          Validated.valid(EventSuccess(TemperatureSettingsSet(TemperatureSettings(min, max))))
        else
          Validated.invalidNec(
            s"Invalid temperature settings: [$min, $max] must be between $minimumTemp and $maximumTemp."
          )
      case GetSmartHome =>
        // TODO improve this response
        Validated.valid(
          CommandResponse(s"Result from ${state.homeId}: ${state.devices} currentTemp: ${state.currentTemperature} motion: ${state.motionState}")
        )

    }
}

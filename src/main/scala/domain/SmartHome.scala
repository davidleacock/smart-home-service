package domain

import java.util.UUID

case class SmartHome(
  homeId: UUID,
  devices: List[Device],
  currentTemperature: Option[Int],
  temperatureSettings: Option[TemperatureSettings],
  motionState: MotionState)

case class TemperatureSettings(minTemp: Int, maxTemp: Int)

sealed trait MotionState

object MotionState {
  case object MotionDetected extends MotionState
  case object MotionNotDetected extends MotionState
}

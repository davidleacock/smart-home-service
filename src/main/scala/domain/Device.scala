package domain

import java.util.UUID

// The current value needs to be more generic

trait Device {
  def id: UUID
  def deviceType: DeviceType
  def updated(newValue: Int): Device
  def currValue: Int
}

trait DeviceType
case object Thermostat extends DeviceType
case object MotionDetector extends DeviceType

// TODO how does Device validation work?
case class Thermostat(
  id: UUID,
  deviceType: DeviceType = Thermostat,
  value: Int)
    extends Device {

  override def updated(newValue: Int): Device =
    // ! arbitrary error value - replace
    if (!newValue.equals(999)) {
      this.copy(value = newValue)
    } else this

  override def currValue: Int = this.value
}

case class MotionDetector(
  id: UUID,
  deviceType: DeviceType = MotionDetector,
  value: Int)
    extends Device {
  override def updated(newValue: Int): Device =
    // ! arbitrary error value - replace
    if (!newValue.equals(888)) {
      this.copy(value = newValue)
    } else this

  override def currValue: Int = this.value
}

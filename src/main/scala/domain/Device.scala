package domain

import java.util.UUID

// The current value needs to be more generic

trait Device {
  def id: UUID
  def deviceType: DeviceType
  def updated(newValue: String): Device
}

trait DeviceType
case object Thermostat extends DeviceType
//case object MotionDetector extends DeviceType

// TODO how does Device validation work?
case class Thermostat(
  id: UUID,
  deviceType: DeviceType = Thermostat,
  value: String)
    extends Device {
  override def updated(newValue: String): Device = {
    if (!newValue.equals("error")) {
      this.copy(value = newValue)
    } else this
  }
}

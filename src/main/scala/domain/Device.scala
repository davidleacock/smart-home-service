package domain

import cats.data.{Validated, ValidatedNec}

import java.util.UUID

sealed trait DeviceValueType {
  def value: Any
}
case class IntDVT(value: Int) extends DeviceValueType
case class StringDVT(value: String) extends DeviceValueType

object DeviceValueTypeImplicits {
  implicit class DeviceValueTypeOps(dvt: DeviceValueType) {
    def unwrapped: Any = dvt match {
      case IntDVT(value)    => value
      case StringDVT(value) => value
    }
  }
}

sealed trait Device {
  val id: UUID
  val currValue: DeviceValueType
  def applyUpdate(newValue: DeviceValueType): ValidatedNec[DeviceError, Device]
}

case class DeviceError(reason: String)

case class Thermostat(
  id: UUID,
  value: Int)
    extends Device {

  // TODO Add more interesting validation to show the power of the invalid type
  override def applyUpdate(newValue: DeviceValueType): ValidatedNec[DeviceError, Device] =
    newValue match {
      case IntDVT(v) if v != 999 => Validated.valid(copy(value = v))
      case _                     => Validated.invalidNec(DeviceError("Invalid value for Thermostat"))
    }

  override val currValue: DeviceValueType = IntDVT(this.value)
}

// TODO Add more interesting validation to show the power of the invalid type
case class MotionDetector(
  id: UUID,
  value: String)
    extends Device {
  override def applyUpdate(newValue: DeviceValueType): ValidatedNec[DeviceError, Device] =
    newValue match {
      case StringDVT(v) if v != "error" => Validated.valid(copy(value = v))
      case _                            => Validated.invalidNec(DeviceError("Invalid value for MotionDetector"))
    }

  override val currValue: DeviceValueType = StringDVT(this.value)
}

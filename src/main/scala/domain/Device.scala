package domain

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
  def applyUpdate(newValue: DeviceValueType): Either[DeviceError, Device]
}

case class DeviceError(reason: String)

case class Thermostat(
  id: UUID,
  value: Int)
    extends Device {

  override def applyUpdate(newValue: DeviceValueType): Either[DeviceError, Device] =
    // ! arbitrary error value - replace with rule validation
    newValue match {
      case IntDVT(v) if v != 999 => Right(copy(value = v))
      case _                     => Left(DeviceError("Invalid value for Thermostat"))
    }

  override val currValue: DeviceValueType = IntDVT(this.value)
}

case class MotionDetector(
  id: UUID,
  value: String)
    extends Device {
  override def applyUpdate(newValue: DeviceValueType): Either[DeviceError, Device] =
    // ! arbitrary error value - replace with rule validation
    newValue match {
      case StringDVT(v) if v != "error" => Right(copy(value = v))
      case _                            => Left(DeviceError("Invalid value for MotionDetector"))
    }

  override val currValue: DeviceValueType = StringDVT(this.value)
}

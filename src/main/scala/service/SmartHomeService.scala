package service

import domain.Device
import service.SmartHomeService.{Command, SmartHomeResult}

import java.util.UUID

trait SmartHomeService[F[_]] {
  def processCommand(homeId: UUID, command: Command): F[SmartHomeResult]
}

object SmartHomeService {
  sealed trait Command
  case class AddDevice(homeId: UUID, device: Device) extends Command

  sealed trait Event
  case class DeviceAdded(homeId: UUID, device: Device) extends Event

  sealed trait SmartHomeResult
  case object Success extends SmartHomeResult
  case object Failure extends SmartHomeResult
}
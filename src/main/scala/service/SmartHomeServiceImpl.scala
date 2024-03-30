package service

import cats.data.State
import cats.effect.IO
import cats.implicits.toFoldableOps
import domain.MotionState.{MotionDetected, MotionNotDetected}
import domain._
import repo.SmartHomeEventRepository
import service.SmartHomeService._

import java.util.UUID

class SmartHomeServiceImpl(repository: SmartHomeEventRepository[IO]) extends SmartHomeService[IO] {

  private sealed trait CommandResult
  private case class EventSuccess(event: Event) extends CommandResult
  private case class CommandResponse(payload: String) extends CommandResult
  private case class CommandFailed(reason: String) extends CommandResult

  override def processCommand(
    homeId: UUID,
    command: Command
  ): IO[SmartHomeResult] =
    for {
      // Retrieve list of events from repo
      events <- repository.retrieveEvents(homeId)
      // Create initial SmartHome State
      initialState = SmartHome(homeId, List.empty, None, None, MotionNotDetected)
      // Replay events to build current State
      currentState = buildState(events).runS(initialState).value
      // Apply command to current state, persist new event if needed and reply
      result <- handleCommand(command, currentState).flatMap {
        case EventSuccess(event) => repository.persistEvent(homeId, event).as(Success) // TODO handle errors from the repo
        case CommandResponse(payload) => IO.pure(ResponseResult(payload))
        case CommandFailed(reason)    => IO.pure(Failure(reason))
      }
    } yield result

  // ! Add validation predicated on state
  // ! Add device specific validation as well
  private def handleCommand(
    command: Command,
    state: SmartHome
  ): IO[CommandResult] = IO {
    command match {
      case AddDevice(device) => EventSuccess(DeviceAdded(device))

      case UpdateDevice(deviceId, newValue) =>
        state.devices.find(_.id == deviceId) match {
          case Some(device) =>
            device.applyUpdate(newValue) match {
              case Right(updatedDevice) => EventSuccess(DeviceUpdated(updatedDevice))
              case Left(error)          => CommandFailed(error.reason)
            }
          case None => CommandFailed(s"Device $deviceId not found.")
        }

      case GetSmartHome =>
        // TODO Improve this response
        CommandResponse(s"Result from ${state.homeId}: ${state.devices} currentTemp: ${state.currentTemperature} motion: ${state.motionState}")

      case SetTemperatureSettings(min, max) =>
        EventSuccess(TemperatureSettingsSet(TemperatureSettings(min, max)))

    }
  }

  private def buildState(events: List[Event]): State[SmartHome, Unit] =
    events.traverse_ { event =>
      State.modify(applyEventToState(event))
    }

  private def applyEventToState(event: Event): SmartHome => SmartHome = { case state @ SmartHome(_, devices, _, _, _) =>
    event match {
      case DeviceAdded(device) =>
        device match {
          case Thermostat(_, temp)  => state.copy(devices = state.devices :+ device, currentTemperature = Some(temp))
          case MotionDetector(_, _) => state.copy(devices = state.devices :+ device)
        }
      case DeviceUpdated(device) =>
        device match {
          case Thermostat(_, temp) =>
            state.copy(devices = updateDevice(devices, device), currentTemperature = Some(temp))
          case MotionDetector(_, motion) =>
            val motionState = motion match {
              case "motion_detected" => MotionDetected
              case "no_motion_detected" => MotionNotDetected
              case _ => MotionNotDetected
            }
            state.copy(devices = updateDevice(devices, device), motionState = motionState)
        }

      case TemperatureSettingsSet(temperatureSettings) => state.copy(temperatureSettings = Some(temperatureSettings))
    }
  }

  private def updateDevice(devices: List[Device], updatedDevice: Device): List[Device] =
    devices.map(device => if (device.id == updatedDevice.id) updatedDevice else device)
}

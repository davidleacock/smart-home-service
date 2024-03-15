package service

import cats.data.State
import cats.effect.IO
import cats.implicits.toFoldableOps
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
      initialState = SmartHome(homeId, List.empty)
      // Replay events to build current State
      currentState = buildState(events).runS(initialState).value
      // Apply command to current state, persist new event if needed and reply
      result <- handleCommand(command, currentState).flatMap {
        case EventSuccess(event)      => repository.persistEvent(homeId, event).as(Success)
        case CommandResponse(payload) => IO.pure(ResponseResult(payload))
        case CommandFailed(reason)    => IO.pure(Failure(reason))
      }
    } yield result

  // ! Add validation predicated on state
  private def handleCommand(
    command: Command,
    state: SmartHome
  ): IO[CommandResult] = IO {
    command match {
      case AddDevice(device) => EventSuccess(DeviceAdded(device))

      case SmartHomeService.UpdateDevice(deviceId, newValue) =>
        state.devices.find(_.id == deviceId) match {
          case Some(device) => EventSuccess(DeviceUpdated(device.updated(newValue)))
          case None         => CommandFailed(s"Device $deviceId not found.")
        }

      case SmartHomeService.GetSmartHome =>
        CommandResponse(s"Result from ${state.homeId}: ${state.devices}")
    }
  }

  private def buildState(events: List[Event]): State[SmartHome, Unit] =
    events.traverse_ { event =>
      State.modify(eventToStateChange(event))
    }

  private def eventToStateChange(event: Event): SmartHome => SmartHome = { case state @ SmartHome(_, devices) =>
    event match {
      case DeviceAdded(device)    => state.copy(devices = state.devices :+ device)
      case DeviceUpdated(device) => state.copy(devices = updateDevice(devices, _ => device))
    }
  }

  private def updateDevice(devices: List[Device], updateFn: Device => Device): List[Device] =
    devices.map {
      case device if device.id == updateFn(device).id => updateFn(device)
      case device                                     => device
    }
}

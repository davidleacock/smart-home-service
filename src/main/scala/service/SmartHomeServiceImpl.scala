package service

import cats.data.State
import cats.effect.IO
import cats.implicits.toFoldableOps
import domain._
import repo.SmartHomeEventRepository
import service.SmartHomeService._

import java.util.UUID

class SmartHomeServiceImpl(repository: SmartHomeEventRepository[IO]) extends SmartHomeService[IO] {

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
      // Apply command to current state and return event
      result <- handleCommand(command, currentState).flatMap {
        case (result, Some(event)) => repository.persistEvent(homeId, event).as(result)
        case (result, None)        => IO.pure(result)
      }
    } yield result

  // ! Add validation predicated on state
  private def handleCommand(
    command: Command,
    state: SmartHome
  ): IO[(SmartHomeResult, Option[Event])] = IO {
    command match {
      case AddDevice(device) =>
        (Success, Some(DeviceAdded(device)))

      case SmartHomeService.UpdateDevice(deviceId, newValue) =>
        state.devices.find(_.id == deviceId) match {
          case Some(device) => (Success, Some(DeviceUpdated(device.updated(newValue))))
          case None         => (Failure(s"Device $deviceId not found."), None)
        }

      case SmartHomeService.GetSmartHome =>
        (Result(s"Result from ${state.homeId}: ${state.devices}"), None)
    }
  }

  private def buildState(events: List[Event]): State[SmartHome, Unit] =
    events.traverse_ { event =>
      State.modify(eventToStateChange(event))
    }

  private def eventToStateChange(event: Event): SmartHome => SmartHome = { case state @ SmartHome(_, devices) =>
    event match {
      case DeviceAdded(device)    => state.copy(devices = state.devices :+ device)
      case DeviceUpdated(updated) => state.copy(devices = updateDevice(devices, _ => updated))
    }
  }

  private def updateDevice(devices: List[Device], updateFn: Device => Device): List[Device] =
    devices.map {
      case device if device.id == updateFn(device).id => updateFn(device)
      case device                                     => device
    }
}

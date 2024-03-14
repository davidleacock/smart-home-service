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
      currentState = applyEvents(events).runS(initialState).value
      // Apply command to current state and return event
      result <- handleCommand(command, currentState).flatMap {
        case (Some(event), result) => repository.persistEvent(homeId, event).as(result)
        case (None, result)        => IO.pure(result)
      }
    } yield result

  // ! Add validation predicated on state
  private def handleCommand(
    command: Command,
    state: SmartHome
  ): IO[(Option[Event], SmartHomeResult)] =
    command match {
      case AddDevice(device) =>
        IO.pure((Some(DeviceAdded(device)), Success))

      case SmartHomeService.UpdateDevice(deviceId, newValue) =>
        IO {
          state.devices.find(_.id == deviceId) match {
            case Some(device) => (Some(DeviceUpdated(device.updated(newValue))), Success)
            case None         => (None, Failure(s"Device $deviceId not found."))
          }
        }

      case SmartHomeService.GetSmartHome =>
        IO.pure(None, Result(s"Result from ${state.homeId}: ${state.devices}"))
    }

  // ! TODO clean up
  private def applyEvents(events: List[Event]): State[SmartHome, Unit] =
    events.traverse_(event => applyEventsToState(event))

  private def applyEventsToState(event: Event): State[SmartHome, Unit] =
    State.modify { state =>
      event match {
        case DeviceAdded(device) =>
          state.copy(
            devices = state.devices :+ device
          )

        case DeviceUpdated(updatedDevice) =>
          val updatedDevices = state
            .devices
            .map { device =>
              if (device.id == updatedDevice.id) {
                updatedDevice
              } else device
            }

          state.copy(devices = updatedDevices)
      }
    }
}

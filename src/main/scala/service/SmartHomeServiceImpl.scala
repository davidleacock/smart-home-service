package service

import cats.data.State
import cats.effect.IO
import cats.implicits.{toFoldableOps, toTraverseOps}
import domain._
import repo.SmartHomeEventRepository
import service.SmartHomeService._

import java.util.UUID

class SmartHomeServiceImpl(repository: SmartHomeEventRepository[IO])
    extends SmartHomeService[IO] {

  override def processCommand(
    homeId: UUID,
    command: Command
  ): IO[SmartHomeResult] = {
    for {
      // Retrieve list of events from repo
      events <- repository.retrieveEvents(homeId)

      // ? I dont think we need the homeId after this, we've got all the events we need.
      // ! Create ACL here? At least needs to happen before handleCommand

      // Create initial SmartHome State
      initialState = SmartHome(homeId, List.empty)
      // Replay events to build current State
      currentState = applyEvents(events).runS(initialState).value
      // Apply command to current state and return event
      result <- handleCommand(command, currentState).flatMap {
        case (Some(event), result) => repository.persistEvent(homeId, event).as(result)
        case (None, result) => IO.pure(result)
      }
    } yield result
  }

  // ! Add validation
  // ? How is the ID being used in all this?
  // ? Any code duplication?
  private def handleCommand(
    command: Command,
    state: SmartHome,
  ): IO[(Option[Event], SmartHomeResult)] = {
    command match {
      case AddDevice(homeId, device) => {
        IO.pure((Some(DeviceAdded(homeId, device)), Success))
      }

      case SmartHomeService.UpdateDevice(homeId, deviceId, newValue) => {
        val device = state.devices.find(_.id == deviceId).get.updated(newValue)
        IO.pure((Some(DeviceUpdated(homeId, device)), Success))
      }

      case SmartHomeService.GetSmartHome(homeId) =>
        IO.pure(None, Result(s"Result from $homeId: ${state.devices}"))
    }
  }

  private def applyEvents(events: List[Event]): State[SmartHome, Unit] =
    events.traverse_(event => applyEventsToState(event))

  // ? do events need the homeId? I think its already set in
  private def applyEventsToState(event: Event): State[SmartHome, Unit] =
    State.modify { state =>
      event match {
        case DeviceAdded(_, device) =>
          state.copy(
            devices = state.devices :+ device
          )

        case DeviceUpdated(_, updatedDevice) =>
          val updatedDevices = state
            .devices
            .map(device => {
              if (device.id == updatedDevice.id) {
                updatedDevice
              } else device
            })

          state.copy(devices = updatedDevices)
      }
    }
}

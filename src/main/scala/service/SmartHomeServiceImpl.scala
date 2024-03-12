package service

import cats.data.State
import cats.effect.IO
import cats.implicits.toTraverseOps
import service.SmartHomeService.{AddDevice, Command, DeviceAdded, Event, SmartHomeResult, Success}
import domain._
import repo.SmartHomeEventRepository

import java.util.UUID

class SmartHomeServiceImpl(repository: SmartHomeEventRepository[IO])
    extends SmartHomeService[IO] {

  override def processCommand(homeId: UUID, command: Command): IO[SmartHomeResult] = {
    for {
      // Retrieve list of events from repo
      events <- repository.retrieveEvents(homeId)
      // Create initial SmartHome State
      initialState = SmartHome(homeId, List.empty)
      // Replay events to build current State
      currentState = events.traverse(applyEventsToState).runS(initialState).value
      // Apply command to current state and return event
      newEvent = commandToEvent(command, currentState)
      // Persist an event TODO this should be option since not all commands may result in an Event
      _ <- repository.persistEvent(homeId, newEvent)

    } yield Success
  }

  private def commandToEvent(command: Command, state: SmartHome): Event = {
    //TODO Add some logic based on the State
    command match {
      case AddDevice(homeId, device) => {
        // TODO add some device validation of sorts
        DeviceAdded(homeId, device)
      }
    }
  }

  private def applyEventsToState(event: Event): State[SmartHome, Unit] = State.modify { state =>
    event match {
      case DeviceAdded(_, device) =>
        state.copy(
          devices = state.devices :+ device
        )
    }
  }
}

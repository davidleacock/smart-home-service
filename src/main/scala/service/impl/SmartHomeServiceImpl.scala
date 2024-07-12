package service.impl

import cats.data.Validated.{Invalid, Valid}
import cats.data.{State, Validated}
import cats.effect.IO
import cats.implicits.toFoldableOps
import domain.MotionState.{MotionDetected, MotionNotDetected}
import domain._
import repo.SmartHomeEventRepository
import rules.SmartHomeRuleEngine
import service.SmartHomeService
import service.SmartHomeService._

import java.util.UUID

class SmartHomeServiceImpl(
  repository: SmartHomeEventRepository[IO],
  ruleEngine: SmartHomeRuleEngine)
    extends SmartHomeService[IO] {

  override def processCommand(
    homeId: UUID,
    command: Command
  ): IO[SmartHomeResult] =
    for {
      // Retrieve list of events from repo
      // ! TODO Now that the repo is streamed from this needs to be wired directly so I can remove the compile.toList part
      events <- repository.retrieveEvents(homeId).compile.toList
      // Create initial SmartHome State
      // ! TODO create a SmartHome.Init (or .New .Empty?) object to clean this up
      initialState = SmartHome(homeId, List.empty, None, None, MotionNotDetected)
      // Replay events to build current State
      currentState = buildState(events).runS(initialState).value
      // Run the command through the rules to see if the state can process it properly
      // Apply command to current state, persist new event if needed and reply
      result <- ruleEngine.validate(command, currentState) match {
        case Valid(cmdResult) =>
          cmdResult match {
            case EventSuccess(event) => repository.persistEvent(homeId, event).as(Success) // TODO handle errors from the repo
            case CommandResponse(payload) => IO.pure(ResponseResult(payload))
            case CommandFailed(reason)    => IO.pure(Failure(reason))
          }
        case Invalid(errors) =>
          IO.pure(Failure(errors.toNonEmptyList.toList.mkString(", ")))
      }
    } yield result

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
              case "motion_detected"    => MotionDetected
              case "no_motion_detected" => MotionNotDetected
              case _                    => MotionNotDetected
            }
            state.copy(devices = updateDevice(devices, device), motionState = motionState)
        }

      case TemperatureSettingsSet(temperatureSettings) => state.copy(temperatureSettings = Some(temperatureSettings))
    }
  }

  private def updateDevice(devices: List[Device], updatedDevice: Device): List[Device] =
    devices.map(device => if (device.id == updatedDevice.id) updatedDevice else device)
}

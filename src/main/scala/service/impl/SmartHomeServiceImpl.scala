package service.impl

import cats.data.Validated.{Invalid, Valid}
import cats.data.{State, Validated}
import cats.effect.IO
import cats.implicits.toFoldableOps
import domain.MotionState.{MotionDetected, MotionNotDetected}
import domain._
import doobie.util.transactor.Transactor
import doobie.implicits._
import repo.impl.postgres.{PostgresOutbox, PostgresSmartHomeEventRepo}
import repo.{Outbox, SmartHomeEventRepository}
import rules.SmartHomeRuleEngine
import service.SmartHomeService
import service.SmartHomeService._

import java.util.UUID

class SmartHomeServiceImpl(
                            smartHomeRepo: PostgresSmartHomeEventRepo,
                            outbox: PostgresOutbox,
                            xa: Transactor[IO],
                            ruleEngine: SmartHomeRuleEngine)
    extends SmartHomeService[IO] {

  override def processCommand(
    homeId: UUID,
    command: Command
  ): IO[SmartHomeResult] =
    for {
      // Retrieve list of events from repo
      // ! TODO Now that the repo is streamed from this needs to be wired directly so I can remove the compile.toList part
      // ? What happens if the list is empty? What is empty Home vs new Home?
      // ? Is it ok to have the transact here? Need to handle errors
      events <- smartHomeRepo.retrieveEvents(homeId).transact(xa).compile.toList
      // Create initial SmartHome State
      // ! TODO create a SmartHome.Init (or .New .Empty?) object to clean this up
      initialState = SmartHome(homeId, List.empty, None, None, MotionNotDetected, None)
      // Replay events to build current State
      currentState = buildState(events).runS(initialState).value
      // Run the command through the rules to see if the state can process it properly
      // Apply command to current state, persist new event if needed and reply
      result <- ruleEngine.validate(command, currentState) match {
        case Valid(cmdResult) =>
          cmdResult match {
            // ! TODO transactionally persist to Outbox, test as well.
            case EventSuccess(event) =>

              val transaction = for {
                _ <- smartHomeRepo.persistEvent(homeId, event) // TODO handle errors from the repo
                _ <- outbox.persistEvent(homeId, event)
              } yield Success

              // TODO - run the transaction, transactionally and if there is an outbox or repo failure
              // we need to handle that gracefully
              transaction.transact(xa).handleErrorWith { err =>
                IO.pure(Failure(s"Failed to process command due to repo error: $err"))
              }

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

  private def applyEventToState(event: Event): SmartHome => SmartHome = { case state @ SmartHome(_, devices, _, _, _, contactInfo) =>
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
      case ContactInfoCreated(contactInfo) => state.copy(contactInfo = Some(contactInfo))
    }
  }

  private def updateDevice(devices: List[Device], updatedDevice: Device): List[Device] =
    devices.map(device => if (device.id == updatedDevice.id) updatedDevice else device)
}

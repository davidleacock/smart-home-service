package rules

import cats.data.Validated
import domain.MotionState.MotionNotDetected
import domain.{IntDVT, SmartHome, TemperatureSettings, Thermostat}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import service.SmartHomeService._

import java.util.UUID

class SmartHomeRuleEngineSpec extends AnyWordSpec with Matchers {

  val ruleEngine = new SmartHomeRuleEngine
  val homeId: UUID = UUID.randomUUID()

  "SmartHomeRuleEngine" should {

    "validate AddDevice command" in {
      val device = Thermostat(UUID.randomUUID(), 25)
      val command = AddDevice(device)
      val state = SmartHome(homeId, List.empty, None, None, MotionNotDetected, None)

      val result = ruleEngine.validate(command, state)
      result shouldBe Validated.valid(EventSuccess(DeviceAdded(device)))
    }

    "validate UpdateDevice command when device exists" in {
      val deviceId = UUID.randomUUID()
      val device = Thermostat(deviceId, 25)
      val command = UpdateDevice(deviceId, IntDVT(30))
      val state = SmartHome(homeId, List(device), None, None, MotionNotDetected, None)

      val result = ruleEngine.validate(command, state)
      result shouldBe Validated.valid(EventSuccess(DeviceUpdated(device.copy(value = 30))))
    }

    "fail to validate UpdateDevice command when device doesn't exist" in {
      val deviceId = UUID.randomUUID()
      val command = UpdateDevice(deviceId, IntDVT(30))
      val state = SmartHome(homeId, List.empty, None, None, MotionNotDetected, None)

      val result = ruleEngine.validate(command, state)
      result shouldBe Validated.invalidNec(s"Device $deviceId not found.")
    }

    "validate SetTemperature command within valid range" in {
      val command = SetTemperatureSettings(10, 20)
      val state = SmartHome(homeId, List.empty, None, None, MotionNotDetected, None)

      val result = ruleEngine.validate(command, state)
      result shouldBe Validated.valid(EventSuccess(TemperatureSettingsSet(TemperatureSettings(10, 20))))
    }

    "fail to validate SetTemperature command outside of valid range" in {
      val command = SetTemperatureSettings(30, 10)
      val state = SmartHome(homeId, List.empty, None, None, MotionNotDetected, None)

      val result = ruleEngine.validate(command, state)
      result shouldBe Validated.invalidNec("Invalid temperature settings: [30, 10] must be between 0 and 50.")
    }
  }
}

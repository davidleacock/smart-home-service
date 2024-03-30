package service

import cats.effect.testing.scalatest.AsyncIOSpec
import domain.DeviceValueTypeImplicits.DeviceValueTypeOps
import domain.{IntDVT, MotionDetector, StringDVT, Thermostat}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import repo.impl.inmem.InMemorySmartHomeEventRepo
import service.SmartHomeService._
import service.impl.SmartHomeServiceImpl

import java.util.UUID

class SmartHomeServiceSpec extends AsyncWordSpec with AsyncIOSpec with Matchers {

  "SmartHomeService" should {

    "process an AddDevice command by adding new device" in {
      val test = for {
        repo <- InMemorySmartHomeEventRepo.create
        service = new SmartHomeServiceImpl(repo)
        homeId = UUID.randomUUID()
        deviceId = UUID.randomUUID()
        device = Thermostat(deviceId, value = 1)

        result <- service.processCommand(homeId, AddDevice(device))
        persistedEvents <- repo.retrieveEvents(homeId)
      } yield {
        result shouldBe Success
        persistedEvents should have size 1
      }

      test.assertNoException
    }

    "process an UpdateDevice command by updating an existing device" in {
      val test = for {
        repo <- InMemorySmartHomeEventRepo.create
        service = new SmartHomeServiceImpl(repo)
        homeId = UUID.randomUUID()
        deviceId = UUID.randomUUID()
        device = Thermostat(deviceId, value = 1)

        _ <- service.processCommand(homeId, AddDevice(device))
        result <- service.processCommand(homeId, UpdateDevice(deviceId, IntDVT(10)))
        persistedEvents <- repo.retrieveEvents(homeId)
      } yield {
        result shouldBe Success
        persistedEvents should have size 2
        persistedEvents.tail.head match {
          case DeviceUpdated(device) => device.currValue.unwrapped shouldBe 10
          case _                     => fail("most recent event isn't DeviceUpdated")
        }
      }

      test.assertNoException
    }

    "process an SetTemperatureSettings command by setting min and max value for SmartHome" in {
      val test = for {
        repo <- InMemorySmartHomeEventRepo.create
        service = new SmartHomeServiceImpl(repo)
        homeId = UUID.randomUUID()
        min = 0
        max = 100

        result <- service.processCommand(homeId, SetTemperatureSettings(min, max))
        persistedEvents <- repo.retrieveEvents(homeId)
      } yield {
        result shouldBe Success
        persistedEvents should have size 1
        persistedEvents.head match {
          case TemperatureSettingsSet(settings) =>
            settings.minTemp shouldBe min
            settings.maxTemp shouldBe max
          case _ => fail("most recent event isn't TemperatureSettingsSet")
        }
      }

      test.assertNoException
    }

    "process an UpdateDevice command for a device that hasn't been updated by returning a Failure" in {
      val test = for {
        repo <- InMemorySmartHomeEventRepo.create
        service = new SmartHomeServiceImpl(repo)
        homeId = UUID.randomUUID()
        deviceId = UUID.randomUUID()

        result <- service.processCommand(homeId, UpdateDevice(deviceId, IntDVT(10)))
      } yield result shouldBe Failure(s"Device $deviceId not found.")

      test.assertNoException
    }

    "process an multiple UpdateDevice command by updating an existing device" in {
      val test = for {
        repo <- InMemorySmartHomeEventRepo.create
        service = new SmartHomeServiceImpl(repo)
        homeId = UUID.randomUUID()
        deviceId = UUID.randomUUID()
        device = Thermostat(deviceId, value = 1)
        _ <- service.processCommand(homeId, AddDevice(device))
        _ <- service.processCommand(homeId, UpdateDevice(deviceId, IntDVT(10)))
        result <- service.processCommand(homeId, UpdateDevice(deviceId, IntDVT(20)))
        persistedEvents <- repo.retrieveEvents(homeId)
      } yield {
        result shouldBe Success
        persistedEvents should have size 3
        persistedEvents.last match {
          case DeviceUpdated(device) => device.currValue.unwrapped shouldBe 20
          case _                     => fail("most recent event isn't DeviceUpdated")
        }
      }

      test.assertNoException
    }

    "process an GetSmartHome command by returning existing SmartHome state" in {
      val test = for {
        repo <- InMemorySmartHomeEventRepo.create
        service = new SmartHomeServiceImpl(repo)
        homeId = UUID.randomUUID()
        deviceId = UUID.randomUUID()
        device = Thermostat(deviceId, value = 1)

        _ <- service.processCommand(homeId, AddDevice(device))
        _ <- service.processCommand(homeId, UpdateDevice(deviceId, IntDVT(10)))
        result <- service.processCommand(homeId, GetSmartHome)
      } yield result shouldBe ResponseResult(s"Result from $homeId: List(Thermostat($deviceId,10)) currentTemp: Some(10) motion: MotionNotDetected")

      test.assertNoException
    }

    "process an Add/UpdateDevice command for multiple devices and return SmartHome state" in {
      val test = for {
        repo <- InMemorySmartHomeEventRepo.create
        service = new SmartHomeServiceImpl(repo)
        homeId = UUID.randomUUID()
        thermostatId = UUID.randomUUID()
        thermostat = Thermostat(thermostatId, value = 1)
        motionId = UUID.randomUUID()
        motion = MotionDetector(motionId, "no_motion_detected")

        _ <- service.processCommand(homeId, AddDevice(thermostat))
        _ <- service.processCommand(homeId, AddDevice(motion))
        _ <- service.processCommand(homeId, UpdateDevice(thermostatId, IntDVT(10)))
        _ <- service.processCommand(homeId, UpdateDevice(motionId, StringDVT("motion_detected")))
        result <- service.processCommand(homeId, GetSmartHome)
      } yield result shouldBe ResponseResult(
        s"Result from $homeId: List(Thermostat($thermostatId,10), MotionDetector($motionId,motion_detected)) currentTemp: Some(10) motion: MotionDetected"
      )

      test.assertNoException
    }

    "ignore an UpdateDevice command when an invalid type is used" in {
      val test = for {
        repo <- InMemorySmartHomeEventRepo.create
        service = new SmartHomeServiceImpl(repo)
        homeId = UUID.randomUUID()
        thermostatId = UUID.randomUUID()
        thermostat = Thermostat(thermostatId, value = 1)

        _ <- service.processCommand(homeId, AddDevice(thermostat))
        _ <- service.processCommand(homeId, UpdateDevice(thermostatId, IntDVT(10)))
        processResult <- service.processCommand(homeId, UpdateDevice(thermostatId, StringDVT("20 deg")))
        result <- service.processCommand(homeId, GetSmartHome)
      } yield {
        processResult shouldBe Failure("Invalid value for Thermostat")
        result shouldBe ResponseResult(s"Result from $homeId: List(Thermostat($thermostatId,10)) currentTemp: Some(10) motion: MotionNotDetected")
      }

      test.assertNoException
    }

    "process an UpdateDevice command for a motion detector" in {
      val test = for {
        repo <- InMemorySmartHomeEventRepo.create
        service = new SmartHomeServiceImpl(repo)
        homeId = UUID.randomUUID()

        motionId = UUID.randomUUID()
        motion = MotionDetector(motionId, "motion_detected")

        _ <- service.processCommand(homeId, AddDevice(motion))
        _ <- service.processCommand(homeId, UpdateDevice(motionId, StringDVT("motion_not_detected")))
        result <- service.processCommand(homeId, GetSmartHome)
      } yield result shouldBe ResponseResult(
        s"Result from $homeId: List(MotionDetector($motionId,motion_not_detected)) currentTemp: None motion: MotionNotDetected"
      )

      test.assertNoException
    }
  }
}

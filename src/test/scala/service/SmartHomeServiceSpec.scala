package service

import cats.effect.testing.scalatest.AsyncIOSpec
import domain.Thermostat
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import repo.impl.InMemorySmartHomeEventRepo
import service.SmartHomeService._

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
        result <- service.processCommand(homeId, UpdateDevice(deviceId, 10))
        persistedEvents <- repo.retrieveEvents(homeId)
      } yield {
        result shouldBe Success
        persistedEvents should have size 2
        persistedEvents.tail.head match {
          case DeviceUpdated(device) => device.currValue shouldBe 10
          case _                     => fail("most recent event isn't DeviceUpdated")
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

        result <- service.processCommand(homeId, UpdateDevice(deviceId, 10))
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
        _ <- service.processCommand(homeId, UpdateDevice(deviceId, 10))
        result <- service.processCommand(homeId, UpdateDevice(deviceId, 20))
        persistedEvents <- repo.retrieveEvents(homeId)
      } yield {
        result shouldBe Success
        persistedEvents should have size 3
        persistedEvents.last match {
          case DeviceUpdated(device) => device.currValue shouldBe 20
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
        _ <- service.processCommand(homeId, UpdateDevice(deviceId, 10))
        result <- service.processCommand(homeId, GetSmartHome)
      } yield result shouldBe ResponseResult(s"Result from $homeId: List(Thermostat($deviceId,Thermostat,10))")

      test.assertNoException
    }
  }
}

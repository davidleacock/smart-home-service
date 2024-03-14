package service

import cats.effect.testing.scalatest.AsyncIOSpec
import domain.Thermostat
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import repo.impl.InMemorySmartHomeEventRepo
import service.SmartHomeService.{AddDevice, DeviceUpdated, Failure, GetSmartHome, Result, Success, UpdateDevice}

import java.util.UUID

class SmartHomeServiceSpec
    extends AsyncWordSpec
    with AsyncIOSpec
    with Matchers {

  "SmartHomeService" should {

    "process an AddDevice command by adding new device" in {
      val test = for {
        repo <- InMemorySmartHomeEventRepo.create
        service = new SmartHomeServiceImpl(repo)
        homeId = UUID.randomUUID()
        deviceId = UUID.randomUUID()

        addResult <- service.processCommand(
          homeId,
          AddDevice(homeId, Thermostat(deviceId, value = 1))
        )
        persistedEvents <- repo.retrieveEvents(homeId)
      } yield {
        addResult shouldBe Success
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

        _ <- service.processCommand(
          homeId,
          AddDevice(homeId, Thermostat(deviceId, value = 1))
        )
        updateResult <- service.processCommand(
          homeId,
          UpdateDevice(homeId, deviceId, 10)
        )
        persistedEvents <- repo.retrieveEvents(homeId)
      } yield {
        updateResult shouldBe Success
        persistedEvents should have size 2
        persistedEvents.tail.head match {
          case DeviceUpdated(_, device) => device.currValue shouldBe 10
          case _ => fail("most recent event isn't DeviceUpdated")
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

        updateResult <- service.processCommand(
          homeId,
          UpdateDevice(homeId, deviceId, 10)
        )
      } yield {
        updateResult shouldBe Failure(s"Device $deviceId not found.")
      }

      test.assertNoException
    }

    "process an multiple UpdateDevice command by updating an existing device" in {
      val test = for {
        repo <- InMemorySmartHomeEventRepo.create
        service = new SmartHomeServiceImpl(repo)
        homeId = UUID.randomUUID()
        deviceId = UUID.randomUUID()

        _ <- service.processCommand(
          homeId,
          AddDevice(homeId, Thermostat(deviceId, value = 1))
        )
        _ <- service.processCommand(
          homeId,
          UpdateDevice(homeId, deviceId, 10)
        )
        updateResult <- service.processCommand(
          homeId,
          UpdateDevice(homeId, deviceId, 20)
        )
        persistedEvents <- repo.retrieveEvents(homeId)
      } yield {
        updateResult shouldBe Success
        persistedEvents should have size 3
        persistedEvents.last match {
          case DeviceUpdated(_, device) => device.currValue shouldBe 20
          case _ => fail("most recent event isn't DeviceUpdated")
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

        _ <- service.processCommand(
          homeId,
          AddDevice(homeId, Thermostat(deviceId, value = 1))
        )
        _ <- service.processCommand(
          homeId,
          UpdateDevice(homeId, deviceId, 10)
        )
        result <- service.processCommand(homeId, GetSmartHome(homeId))
      } yield {
        result shouldBe Result(s"Result from $homeId: List(Thermostat($deviceId,Thermostat,10))")

      }

      test.assertNoException
    }
  }
}

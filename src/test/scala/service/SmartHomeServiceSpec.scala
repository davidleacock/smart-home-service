package service

import cats.effect.testing.scalatest.AsyncIOSpec
import domain.Thermostat
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import repo.impl.InMemorySmartHomeEventRepo
import service.SmartHomeService.{AddDevice, Success}

import java.util.UUID

class SmartHomeServiceSpec extends AsyncFlatSpec with AsyncIOSpec with Matchers {


  "SmartHomeService" should "process command and persist event" in {

    val test = for {
      repo <- InMemorySmartHomeEventRepo.create
      service = new SmartHomeServiceImpl(repo)
      homeId = UUID.randomUUID()

      addResult <- service.processCommand(homeId, AddDevice(homeId, Thermostat(homeId, value = 1)))
      persistedEvents <- repo.retrieveEvents(homeId)
    } yield {
      addResult shouldBe Success
      persistedEvents should have size 1
    }

    test.assertNoException
  }
}

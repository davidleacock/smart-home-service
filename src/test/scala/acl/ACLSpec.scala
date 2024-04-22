package acl

import domain.{IntDVT, Thermostat}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import service.SmartHomeService.{AddDevice, UpdateDevice}

import java.util.UUID

class ACLSpec extends AnyWordSpec with Matchers {

  // ! This will get updated once I switch to proto definitions for external api
  "ACL should correctly parse a valid string into an AddDevice command" in {

    val deviceId = UUID.randomUUID()
    val newValue = 22

    val jsonEvent =
      s"""
         |{
         |  "cmdType":"AddDevice",
         |  "payload": {
         |    "deviceType": "Thermostat",
         |    "deviceId": "$deviceId",
         |    "initialValue": {
         |      "type": "int",
         |      "value": $newValue
         |    }
         |  }
         |}
         |""".stripMargin


    val result = ACL.parseEvent(jsonEvent)

    result match {
      case Right(AddDevice(Thermostat(id, value))) =>
        id shouldBe deviceId
        value shouldBe newValue
      case Left(error) => fail(s"ACL did not properly parse event to return UpdateDevice - $error")
    }
  }

  "ACL should correctly parse a valid string into an UpdateDevice command" in {

    val deviceId = UUID.randomUUID()
    val updatedValue = 22

    val jsonEvent =
      s"""
         |{
         |  "cmdType":"UpdateDevice",
         |  "payload": {
         |    "deviceId": "$deviceId",
         |    "newValue": {
         |      "type": "int",
         |      "value": $updatedValue
         |    }
         |  }
         |}
         |""".stripMargin


    val result = ACL.parseEvent(jsonEvent)

    result match {
      case Right(UpdateDevice(id, value)) =>
        id shouldBe deviceId
        value shouldBe IntDVT(updatedValue)
      case Left(error) => fail(s"ACL did not properly parse event to return UpdateDevice - $error")
    }
  }

  "ACL should properly handle an invalid string by returning an error" in {
    // ! Once Error type is defined write this test
  }

}

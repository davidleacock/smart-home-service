package acl

import domain.IntDVT
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import service.SmartHomeService.UpdateDevice

import java.util.UUID

class ACLSpec extends AnyWordSpec with Matchers {

  // ! This will get updated once I switch to proto definitions for external api
  "ACL should correctly parse a valid string into an UpdateDevice command" in {

    val deviceId = UUID.randomUUID()
    val newValue = IntDVT(10)

    val jsonEvent =
      s"""
         |{
         |  "cmdType":"UpdateDevice",
         |  "payload": {
         |    "homeId": "${UUID.randomUUID()}",
         |    "deviceId": "$deviceId",
         |    "newValue": {
         |      "type": "IntDVT",
         |      "value": 10
         |    }
         |  }
         |}
         |""".stripMargin


    val result = ACL.parseEvent(jsonEvent)

    result match {
      case Right(UpdateDevice(id, value)) =>
        id shouldBe deviceId
        value shouldBe newValue
      case Left(error) => fail(s"ACL did not properly parse event to return UpdateDevice - $error")
    }

  }

  "ACL should properly handle an invalid string by returning an error" in {
    // ! Once Error type is defined write this test
  }

}

package repo.impl.postgres

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import domain.{MotionDetector, TemperatureSettings, Thermostat}
import doobie.Transactor
import doobie.util.transactor.Transactor.Aux
import org.flywaydb.core.Flyway
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import service.SmartHomeService.{DeviceAdded, DeviceUpdated, TemperatureSettingsSet}

import java.util.UUID

class PostgresSmartHomeEventRepoSpec extends AnyWordSpec with Matchers with ForAllTestContainer with BeforeAndAfterAll {

  override val container: PostgreSQLContainer = PostgreSQLContainer()

  lazy val transactor: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    container.jdbcUrl,
    container.username,
    container.password
  )

  override def afterStart(): Unit = {
    super.afterStart()
    Flyway
      .configure()
      .dataSource(container.jdbcUrl, container.username, container.password)
      .locations("classpath:db/migrations")
      .load()
      .migrate()
  }

  "PostgresSmartHomeEventRepo" should {

    "persist and retrieve thermostat device events correctly" in {
      val repo = new PostgresSmartHomeEventRepo(transactor)

      val homeId = UUID.randomUUID()
      val deviceId = UUID.randomUUID()

      val device = Thermostat(deviceId, 5)
      val event = DeviceAdded(device)

      repo.persistEvent(homeId, event).unsafeRunSync()

      val updateDeviceEvent = DeviceUpdated(Thermostat(deviceId, 10))
      repo.persistEvent(homeId, updateDeviceEvent).unsafeRunSync()

      val result = repo.retrieveEvents(homeId).unsafeRunSync()

      result should be(List(event, updateDeviceEvent))
    }

    "persist and retrieve temperature setting events" in {
      val repo = new PostgresSmartHomeEventRepo(transactor)

      val homeId = UUID.randomUUID()
      val min = 0
      val max = 100

      val event = TemperatureSettingsSet(TemperatureSettings(min, max))

      repo.persistEvent(homeId, event).unsafeRunSync()

      val result = repo.retrieveEvents(homeId).unsafeRunSync()

      result should be(List(event))
    }

    "persist and retrieve motion detector events correctly" in {
      val repo = new PostgresSmartHomeEventRepo(transactor)

      val homeId = UUID.randomUUID()
      val deviceId = UUID.randomUUID()

      val device = MotionDetector(deviceId, "motion_not_detected")
      val event1 = DeviceAdded(device)

      repo.persistEvent(homeId, event1).unsafeRunSync()

      val updatedDevice = MotionDetector(deviceId, "motion_detected")
      val event2 = DeviceUpdated(updatedDevice)

      repo.persistEvent(homeId, event2).unsafeRunSync()

      val result = repo.retrieveEvents(homeId).unsafeRunSync()

      result should be(List(event1, event2))
    }
  }
}

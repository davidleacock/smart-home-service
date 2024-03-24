package repo.impl.postgres

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import domain.Thermostat
import doobie.Transactor
import org.flywaydb.core.Flyway
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import service.SmartHomeService.{DeviceAdded, DeviceUpdated}

import java.util.UUID

class PostgresSmartHomeEventRepoSpec extends AnyFlatSpec with Matchers with TestContainerForAll {

  override val containerDef: PostgreSQLContainer.Def = PostgreSQLContainer.Def()

  it should "persist and retrieve events correctly" in withContainers { pg: PostgreSQLContainer =>

    Flyway
      .configure()
      .dataSource(pg.jdbcUrl, pg.username, pg.password)
      .locations("classpath:db/migrations")
      .load()
      .migrate()

    val transactor = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      pg.jdbcUrl,
      pg.username,
      pg.password
    )

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

    val home2Id = UUID.randomUUID()
    val deviceId2 = UUID.randomUUID()
    val thermo2 = Thermostat(deviceId2, 25)
    val event2 = DeviceAdded(thermo2)

    repo.persistEvent(home2Id, event2).unsafeRunSync()

    repo.retrieveEvents(home2Id).unsafeRunSync() should contain(event2)
  }
}

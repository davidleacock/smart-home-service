package repo.impl.postgres

import cats.effect.IO
import doobie.Transactor
import doobie.implicits._
import io.circe.syntax.EncoderOps
import repo.SmartHomeEventRepository
import service.SmartHomeService.Event
import fs2.Stream

import java.util.UUID

class PostgresSmartHomeEventRepo(val xa: Transactor[IO]) extends SmartHomeEventRepository[IO] {

  import utils.EncoderDecoder._

  override def persistEvent(homeId: UUID, event: Event): IO[Unit] = {
    val eventData = event.asJson.noSpaces
    val eventType = event.getClass.getSimpleName
    val id = homeId.toString

    sql"""
         INSERT INTO events (home_id, event_type, event_data)
         VALUES ($id, $eventType, $eventData)
         """
      .update
      .run
      .transact(xa)
      .void
  }

  override def retrieveEvents(homeId: UUID): Stream[IO, Event] = {
    sql"""
         SELECT event_data FROM events WHERE home_id = ${homeId.toString}
       """
      .query[Event]
      .stream
      .transact(xa)
  }
}



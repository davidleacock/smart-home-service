package repo.impl.postgres

import doobie.ConnectionIO
import doobie.implicits._
import fs2.Stream
import io.circe.syntax.EncoderOps
import repo.SmartHomeEventRepository
import service.SmartHomeService.Event

import java.util.UUID

class PostgresSmartHomeEventRepo extends SmartHomeEventRepository[ConnectionIO] {

  import utils.EncoderDecoder._

  override def persistEvent(homeId: UUID, event: Event): ConnectionIO[Int] = {
    val eventData = event.asJson.noSpaces
    val eventType = event.getClass.getSimpleName
    val id = homeId.toString

    sql"""
         INSERT INTO events (home_id, event_type, event_data)
         VALUES ($id, $eventType, $eventData)
         """
      .update
      .run
  }

  override def retrieveEvents(homeId: UUID): Stream[ConnectionIO, Event] = {
    sql"""
         SELECT event_data FROM events WHERE home_id = ${homeId.toString}
       """
      .query[Event]
      .stream
  }
}



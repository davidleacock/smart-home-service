package repo.impl.postgres

import cats.effect.IO
import doobie.{ConnectionIO, Transactor}
import doobie.implicits._
import io.circe.syntax.EncoderOps
import repo.{Outbox, SmartHomeEventRepository}
import service.SmartHomeService.Event
import fs2.Stream

import java.util.UUID

class PostgresOutbox extends Outbox[ConnectionIO]{

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
}

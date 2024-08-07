package repo

import service.SmartHomeService.Event
import fs2.Stream

import java.util.UUID

trait SmartHomeEventRepository[F[_]] {
  def persistEvent(homeId: UUID, event: Event): F[Int]
  def retrieveEvents(homeId: UUID): Stream[F, Event]
}
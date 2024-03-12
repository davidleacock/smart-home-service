package repo

import service.SmartHomeService.Event

import java.util.UUID

trait SmartHomeEventRepository[F[_]] {
  def persistEvent(homeId: UUID, event: Event): F[Unit]
  def retrieveEvents(homeId: UUID): F[List[Event]]
}
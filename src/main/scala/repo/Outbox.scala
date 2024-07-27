package repo

import service.SmartHomeService.Event

import java.util.UUID

trait Outbox[F[_]] {
  def persistEvent(homeId: UUID, event: Event): F[Int]
}

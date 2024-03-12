package repo.impl

import cats.effect.IO
import domain.SmartHomeService.Event
import repo.SmartHomeEventRepository

import java.util.UUID

class InMemorySmartHomeRepoEvent extends SmartHomeEventRepository[IO] {

//  private val storage: mutable.Map[UUID, SmartHome] = mutable.Map.empty

  // TODO figure out mutable storage for this event store

  override def persistEvent(homeId: UUID, event: Event): IO[Unit] = ???

  override def retrieveEvents(homeId: UUID): IO[List[Event]] = ???
}

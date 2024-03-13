package repo.impl

import cats.effect.IO
import service.SmartHomeService.Event
import repo.SmartHomeEventRepository

import java.util.UUID
import scala.collection.mutable

class InMemorySmartHomeRepoEvent extends SmartHomeEventRepository[IO] {

  private val storage: mutable.Map[UUID, List[Event]] = mutable.Map.empty

  // ! Investigate Ref usage for better concurrency
  override def persistEvent(homeId: UUID, event: Event): IO[Unit] = IO {
    storage.get(homeId) match {
      case Some(events) => events :+ event
      case None => storage + (homeId -> List(event))
    }
  }

  override def retrieveEvents(homeId: UUID): IO[List[Event]] =
    IO {
      storage.getOrElse(homeId, List.empty)
    }
}

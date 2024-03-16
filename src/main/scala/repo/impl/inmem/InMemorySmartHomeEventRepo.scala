package repo.impl.inmem

import cats.effect.{IO, Ref}
import repo.SmartHomeEventRepository
import service.SmartHomeService.Event

import java.util.UUID
import scala.collection.mutable

class InMemorySmartHomeEventRepo private(
  storage: Ref[IO, mutable.Map[UUID, List[Event]]])
    extends SmartHomeEventRepository[IO] {

  override def persistEvent(homeId: UUID, event: Event): IO[Unit] =
    storage.update { current =>
      current.get(homeId) match {
        case Some(events) => current.clone().addOne(homeId, events :+ event)
        case None         => current + (homeId -> List(event))
      }
    }

  override def retrieveEvents(homeId: UUID): IO[List[Event]] =
    storage.get.map(_.getOrElse(homeId, List.empty))
}

object InMemorySmartHomeEventRepo {
  def create: IO[InMemorySmartHomeEventRepo] = {
    Ref.of[IO, mutable.Map[UUID, List[Event]]](mutable.Map.empty).map(storage => new InMemorySmartHomeEventRepo(storage))
  }
}

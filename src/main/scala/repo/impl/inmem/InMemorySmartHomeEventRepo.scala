package repo.impl.inmem

import cats.effect.{IO, Ref}
import repo.SmartHomeEventRepository
import service.SmartHomeService.Event
import fs2.Stream

import java.util.UUID
import scala.collection.mutable

class InMemorySmartHomeEventRepo private (
  storage: Ref[IO, mutable.Map[UUID, List[Event]]])
    extends SmartHomeEventRepository[IO] {

  override def persistEvent(homeId: UUID, event: Event): IO[Unit] =
    storage.update { current =>
      val updatedEvents = current.getOrElse(homeId, List.empty) :+ event
      current + (homeId -> updatedEvents)
    }

  override def retrieveEvents(homeId: UUID): Stream[IO, Event] =
    Stream.eval(storage.get).flatMap { current =>
      Stream.emits(current.getOrElse(homeId, List.empty))
    }
}

object InMemorySmartHomeEventRepo {
  def create: IO[InMemorySmartHomeEventRepo] =
    Ref.of[IO, mutable.Map[UUID, List[Event]]](mutable.Map.empty).map(storage => new InMemorySmartHomeEventRepo(storage))
}

package consumers.impl.inmem

import cats.effect.IO
import fs2.Stream
import consumers.EventConsumer

import scala.collection.mutable

class InMemoryEventConsumer extends EventConsumer[IO]{

  private val eventQueue: mutable.Queue[String] = mutable.Queue.empty[String]

  def enqueueEvent(event: String): mutable.Queue[String] = {
    eventQueue.enqueue(event)
  }

  // Just grab the first element off the topic, regardless of what it is.
  override def consumeEvent(topic: String): fs2.Stream[IO, String] = {
    Stream.unfoldEval[IO, mutable.Queue[String], String](eventQueue) {
      queue =>
        IO {
          queue.dequeueFirst(_ => true).map(event => (event, queue))
        }
    }
  }
}

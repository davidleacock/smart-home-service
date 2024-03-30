package consumers

import fs2.Stream

trait EventConsumer[F[_]] {
  def consumeEvent(topic: String): Stream[F, String]
}

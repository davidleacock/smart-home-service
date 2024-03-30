package consumers.impl

import cats.effect.IO
import consumers.EventConsumer

class KafkaEventConsumer(server: String, groupId: String) extends EventConsumer[IO]{



  override def consumeEvent(topic: String): fs2.Stream[IO, String] = ???
}

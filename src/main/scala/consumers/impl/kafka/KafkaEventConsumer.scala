package consumers.impl.kafka

import cats.effect.IO
import consumers.EventConsumer
import fs2.kafka.{AutoOffsetReset, ConsumerSettings, KafkaConsumer}

import scala.concurrent.duration.DurationInt

class KafkaEventConsumer(server: String, groupId: String, clientId: String) extends EventConsumer[IO] {

  private val consumerSettings: ConsumerSettings[IO, String, String] =
    ConsumerSettings[IO, String, String]
      .withBootstrapServers(server)
      .withAutoOffsetReset(AutoOffsetReset.Latest)
      .withGroupId(groupId)
      .withEnableAutoCommit(true)
      .withAutoCommitInterval(5.seconds)
      .withClientId(clientId)

  // ! TODO Error handling
  override def consumeEvent(topic: String): fs2.Stream[IO, String] =
    KafkaConsumer
      .stream(consumerSettings)
      .evalTap(consumer => consumer.subscribeTo(topic))
      .flatMap(consumer => consumer.stream)
      .map(consumerRecord => consumerRecord.record.value)
}

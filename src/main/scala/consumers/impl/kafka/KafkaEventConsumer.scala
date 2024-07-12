package consumers.impl.kafka

import cats.effect.IO
import consumers.EventConsumer
import org.typelevel.log4cats.slf4j.Slf4jLogger
import fs2.kafka._

import scala.concurrent.duration.DurationInt

class KafkaEventConsumer(server: String, groupId: String, clientId: String) extends EventConsumer[IO] {
  private val logger = Slf4jLogger.getLogger[IO]

  private val consumerSettings: ConsumerSettings[IO, String, String] =
    ConsumerSettings[IO, String, String]
      .withBootstrapServers(server)
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withGroupId(groupId)
      .withEnableAutoCommit(true)
      .withAutoCommitInterval(5.seconds)
      .withClientId(clientId)


  // ! TODO Error handling
  override def consumeEvent(topic: String): fs2.Stream[IO, String] =
    KafkaConsumer
      .stream(consumerSettings)
      .evalTap(_ => logger.info(s"Subscribing to topic ${topic}"))
      .evalTap(consumer => consumer.subscribeTo(topic))
      .flatMap(_.stream)
      .map((record: CommittableConsumerRecord[IO, String, String]) => {
        println(s"Received: $record")
        record.record.value
      })
      .evalMap(value => IO(println(s"Received: $value")).as(value))
}

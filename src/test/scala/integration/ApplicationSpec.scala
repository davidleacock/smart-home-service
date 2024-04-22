package integration

import acl.ACL
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import domain.IntDVT
import org.flywaydb.core.Flyway

import java.util.Properties
import java.util.concurrent.TimeUnit
import scala.jdk.CollectionConverters._
//import scala.compat.java8.FutureConverters
//import scala.compat.java8.FutureConverters._

//import java.util.concurrent.Future
//import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import com.dimafeng.testcontainers.lifecycle.and
import com.dimafeng.testcontainers.scalatest.TestContainersForAll
import com.dimafeng.testcontainers.{KafkaContainer, PostgreSQLContainer}
import consumers.impl.kafka.KafkaEventConsumer
import doobie.Transactor
import doobie.util.transactor.Transactor.Aux
import org.apache.kafka.clients.admin.{AdminClient, AdminClientConfig, NewTopic}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import repo.impl.postgres.PostgresSmartHomeEventRepo
import rules.SmartHomeRuleEngine
import service.SmartHomeService
import service.impl.SmartHomeServiceImpl

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/*
    Note:  Since there currently is no runnable Application as I build this thing out. I'm using the ApplicationSpec
    as a place to wire up the pieces and test them out, fix things, make changes etc.  Once I'm happy with the design
    then I can create the runnable Main
 */

class ApplicationSpec
    extends AsyncWordSpec
    with AsyncIOSpec
    with TestContainersForAll
    with BeforeAndAfterAll
    with Matchers {

  override type Containers = PostgreSQLContainer and KafkaContainer

  override def startContainers(): Containers = {
    val container1 = PostgreSQLContainer.Def().start()
    val container2 = KafkaContainer.Def().start()

    container1 and container2
  }


  override def afterContainersStart(container: Containers): Unit = {
    super.afterContainersStart(container)

    // Once containers are running we need to migrate the db and setup kafka
    container match {
      case pg and kf =>
        // DB Migration
        Flyway
          .configure()
          .dataSource(pg.container.getJdbcUrl, pg.container.getUsername, pg.container.getPassword)
          .locations("classpath:db/migrations")
          .load()
          .migrate()

        val adminProps = new Properties()
        adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kf.container.getBootstrapServers)
        val adminClient = AdminClient.create(adminProps)

        // Create topics
        val topic = new NewTopic("device-events-topic", 1, 1.toShort)
        adminClient.createTopics(List(topic).asJava).all().get(30, SECONDS)

        println(s"Containers ${pg.username} / ${pg.password} and $kf")
    }
  }

  override implicit def executionContext: ExecutionContext = ExecutionContext.global

  "SmartHome Application" should {

    "process commands" in withContainers { case pg and kf =>
      lazy val producer = {
        val props = Map[String, Object](
          ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> kf.container.getBootstrapServers,
          ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG -> classOf[StringSerializer].getName,
          ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG -> classOf[StringSerializer].getName
        ).asJava

        new KafkaProducer[String, String](props)
      }

      def publishToKafka(topic: String, message: String): IO[Unit] =
        IO {
          val record = new ProducerRecord[String, String](topic, "key", message)
          producer.send(record).get(30L, TimeUnit.SECONDS)
        }.handleErrorWith { e =>
          IO(println(s"Failed to send message due to: ${e.getMessage}"))
        }.void

      lazy val transactor: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
        "org.postgresql.Driver",
        pg.jdbcUrl,
        pg.username,
        pg.password
      )

      val rules = new SmartHomeRuleEngine
      val repo = new PostgresSmartHomeEventRepo(transactor)
      val smartHomeService = new SmartHomeServiceImpl(repo, rules)
      val kafkaConsumer = new KafkaEventConsumer(
        server = kf.container.getBootstrapServers,
        groupId = "application-spec-id",
        clientId = "application-spec-client-id"
      )


      val deviceId = UUID.randomUUID()
      val newValue = 10

      val testMessage =
        s"""
           |{
           |  "cmdType":"AddDevice",
           |  "payload": {
           |    "deviceType": "Thermostat",
           |    "deviceId": "$deviceId",
           |    "initialValue": {
           |      "type": "int",
           |      "value": $newValue
           |    }
           |  }
           |}
           |""".stripMargin

      val scenario = for {
        _ <- publishToKafka("device-events-topic", testMessage)
        result <- kafkaConsumer
          .consumeEvent("device-events-topic")
          .evalMap { event =>
            println(s"got events $event")
            for {
              command <- IO.fromEither(ACL.parseEvent(event))
              result <- smartHomeService.processCommand(UUID.randomUUID(), command)
            } yield result
          }
          .take(1)
          .compile
          .lastOrError
      } yield result

      val test = scenario.unsafeToFuture().map {
        case SmartHomeService.Success                  => succeed
        case SmartHomeService.ResponseResult(response) => fail(s"Unexpected return type: ResponseResult $response")
        case SmartHomeService.Failure(reason)          => fail(s"Unexpected return type: Failure $reason")
      }

      test.assertNoException
    }
  }
}

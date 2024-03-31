package integration

import acl.ACL
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.dimafeng.testcontainers.{ForAllTestContainer, MultipleContainers, PostgreSQLContainer}
import consumers.impl.kafka.KafkaEventConsumer
import doobie.Transactor
import doobie.util.transactor.Transactor.Aux
import org.flywaydb.core.Flyway
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import repo.impl.postgres.PostgresSmartHomeEventRepo
import service.SmartHomeService.Success
import service.impl.SmartHomeServiceImpl

import java.util.UUID
import scala.concurrent.ExecutionContext

class ApplicationSpec
    extends AsyncWordSpec
    with AsyncIOSpec
    with ForAllTestContainer
    with BeforeAndAfterAll
    with Matchers {

  val postgresContainer: PostgreSQLContainer = PostgreSQLContainer()
  val kafkaContainer: KafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"))

  override implicit def executionContext: ExecutionContext = ExecutionContext.global

  lazy val transactor: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    postgresContainer.jdbcUrl,
    postgresContainer.username,
    postgresContainer.password
  )

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    Flyway
      .configure()
      .dataSource(postgresContainer.jdbcUrl, postgresContainer.username, postgresContainer.password)
      .locations("classpath:db/migrations")
      .load()
      .migrate()
  }

  override def container: MultipleContainers = MultipleContainers(postgresContainer, kafkaContainer)

  // ! Not ready to be used yet
  // ! Need to push events to kafka as part of running test infrastructure (setup topic, etc)

  "SmartHome Application" should {

    "consume external messages and process them" in {

      val repo = new PostgresSmartHomeEventRepo(transactor)
      val smartHomeService = new SmartHomeServiceImpl(repo)
      val kafkaConsumer = new KafkaEventConsumer(
        server = kafkaContainer.getBootstrapServers,
        groupId = "application-spec-id",
        clientId = "application-spec-client-id"
      )

      val scenario = kafkaConsumer.consumeEvent("device-events-topic").evalMap { event =>
        for {
          command <- IO.fromEither(ACL.parseEvent(event))
          result <- smartHomeService.processCommand(UUID.randomUUID(), command)
        } yield result
      }

      val test = scenario
        .attempt
        .compile
        .toList
        .unsafeToFuture()
        .map { results =>
          results.foreach {
            case Right(result) => result shouldBe Success
            case Left(error)   => fail(s"$error")
          }
        }

      test.assertNoException
    }

  }
}

# smart-home-service
IoT SmartHome Application built with Typelevel Ecosystem

State: Work In Progress (mind the mess...)

Built with the Typelevel ecosystem, this service orchestrates smart home devices by processing commands, updating the SmartHome state, and maintaining a comprehensive event log for state changes. We employ CQRS to separate the read and write operations and use Kafka as the message broker to ensure service isolation.

Current Focus: Event Sourcing
We're implementing the event sourcing pattern to underpin our state management approach. By capturing all state changes as events and persisting them in an event store, we create an audit trail of activities within the home. This event log is crucial for reconstructing the SmartHome state, enabling accurate processing of incoming commands. State management and effectful I/O are elegantly handled using the Cats library, showcasing the power and flexibility of functional programming in Scala.
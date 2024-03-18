# smart-home-service
### IoT SmartHome Application built with Typelevel Ecosystem

## State: Work In Progress (mind the mess...)

This service orchestrates smart home devices by processing commands, updating the SmartHome state, and maintaining a comprehensive event log for state changes. We employ CQRS to separate the read and write operations and use Kafka as the message broker to ensure service isolation.

### SmartHomeService API
`AddDevice(homeId: UUID, device: Device)` </p>
`UpdateDevice(homeId: UUID, deviceId: UUID, newValue: DeviceValueType`


## In Progress: Event Sourcing
We're implementing the event sourcing pattern to underpin our state management approach. By capturing all state changes as events and persisting them in an event store, we create an audit trail of activities within the home. This event log is crucial for reconstructing the SmartHome state, enabling accurate processing of incoming commands. 

## In Progress: Postgres Repo
Add postgres repo that can be used in an integration test to verify schema and correct usage of doobie library


### Remaining work...
* Add postgres repo
* integration tests with postgres repo
* Add FSM to SmartHome 
* Create rule engine for SmartHome and Devices for validation
* Kafka ingress to ACL
* Postgres projection to Kafka egress
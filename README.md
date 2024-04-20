# smart-home-service
### IoT SmartHome Application built with Typelevel Ecosystem

## State: Work In Progress (mind the mess...)

This service orchestrates smart home devices by processing commands, updating the SmartHome state, and maintaining a comprehensive event log for state changes. We employ CQRS to separate the read and write operations and use Kafka as the message broker to ensure service isolation.

 Currently thinking that aspects of the SmartHome API will be accessible via a REST/gRPC endpoint, things like Adding Devices, setting 
properties of the SmartHome itself (min max allowed temperatures, personal settings for alerts, etc) and then information about Devices will be consumed via Kafka.


![diagram.png](diagram.png)


### SmartHomeService API
`AddDevice(homeId: UUID, device: Device)` </p>
`UpdateDevice(homeId: UUID, deviceId: UUID, newValue: DeviceValueType)` </p>
`GetSmartHome(homeId: UUID)` </p>
`SetTemperatureSettings(min: Int, max: Int)` </p>


## In Progress: Kafka ingress consumer <p>
Will use protobuf to define some external event that the service will consume from via Kafka, then translate that into the SmartHome api. Currently, I'm thinking that information about devices will come through the kafka ingress like we're reading stream of data from outside devices, whereas adding devices and adding properties about the home will come through a grpc or rest endpoint?




### Remaining work...
* Add postgres repo ✅
* integration tests with postgres repo ✅
* Add FSM to SmartHome ✅
* Create rule engine for SmartHome (✅) and Devices for validation
* Create proto definition for external api (representing device events from the outside world)
* Kafka ingress to ACL ✅
* Postgres projection to Kafka egress
* Ensure complete separation of write and query side of the data model
* Replace InMem implementations in integration tests with containerized versions (postgres, kakfa, etc...)
* Use `Resource` for dependency injection?


### Finished
~~## In Progress: Postgres Repo
Add postgres repo that can be used in an integration test to verify schema and correct usage of doobie library~~ <p>
~~## In Progress: Event Sourcing
We're implementing the event sourcing pattern to underpin our state management approach. By capturing all state changes as events and persisting them in an event store, we create an audit trail of activities within the home. This event log is crucial for reconstructing the SmartHome state, enabling accurate processing of incoming commands.~~
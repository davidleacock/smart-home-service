package domain

import java.util.UUID

case class SmartHome(
  homeId: UUID,
  devices: List[Device],
  currentTemperature: Option[Int],
  temperatureSettings: Option[TemperatureSettings])

case class TemperatureSettings(minTemp: Int, maxTemp: Int)

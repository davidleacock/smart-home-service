package domain

import java.util.UUID

case class SmartHome(homeId: UUID, devices: List[Device])
package rules

import cats.data.Validated
import cats.data.ValidatedNec
import cats.implicits._
import domain.SmartHome
import service.SmartHomeService.Command

type ValidationError = String

trait RuleEngine[C, S, R] {
  def validate(command: C, state: S): ValidatedNec[ValidationError, R]
}

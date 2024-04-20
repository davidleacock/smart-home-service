package rules

import cats.data.ValidatedNec

trait RuleEngine[C, S, R] {
  type ValidationError = String

  def validate(command: C, state: S): ValidatedNec[ValidationError, R]
}

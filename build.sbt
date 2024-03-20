ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.13"

lazy val root = (project in file("."))
  .settings(
    name := "smart-home-service",
    libraryDependencies += "org.typelevel" %% "cats-core" % "2.10.0"
  )

// TODO clean this up once all libraries added

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "3.2.17",
  "org.scalatest" %% "scalatest" % "3.2.17" % "test",
  "org.typelevel" %% "cats-effect" % "3.5.2",
  "org.scalatest" %% "scalatest" % "3.2.17",
  "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0",
  "org.tpolecat" %% "doobie-core" % "1.0.0-RC1",
  "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC1",
  "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC1",
  "io.circe" %% "circe-core" % "0.14.1",
  "io.circe" %% "circe-generic" % "0.14.1",
  "io.circe" %% "circe-parser" % "0.14.1",
  "io.circe" %% "circe-generic-extras" % "0.14.1",
  "org.testcontainers" % "postgresql" % "1.15.3",
  "org.flywaydb" % "flyway-core" % "7.11.1",
  "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.39.6" % "test",
  "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.39.6" % "test"
)

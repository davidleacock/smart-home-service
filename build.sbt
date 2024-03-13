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
  "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0"
)

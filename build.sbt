import Dependencies._

// give the user a nice default project!
ThisBuild / organization := "com.github.fehu"
ThisBuild / scalaVersion := "2.13.3"
ThisBuild / version      := "0.1.0-SNAPSHOT"

addCommandAlias("fullDependencyUpdates", ";dependencyUpdates; reload plugins; dependencyUpdates; reload return")

inThisBuild(Seq(
  addCompilerPlugin(Plugin.`better-monadic-for`),
  addCompilerPlugin(Plugin.`kind-projector`)
))

lazy val root = (project in file("."))
  .settings(
    name := "livecam",
    resolvers += Repos.dcm4che,
    libraryDependencies ++= Seq(
      `cats-core`,
      `cats-effect`,
      `fs2-core`,
      `log4cats-slf4j`,
      `slf4j-simple`,
       xuggler,
      `scala-test` % Test
    )
  )

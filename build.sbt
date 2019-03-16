import sbt._
import sbt.Keys._

lazy val buildSettings = Seq(
          organization := BuildSettings.organization,
          scalaVersion := Dependencies.Versions.scala,
         scalacOptions ++= BuildSettings.compilerFlags,
            crossPaths := false,
         sourcesInBase := false,
     externalResolvers := BuildSettings.resolvers
)

lazy val root = project.in(file("."))
  .settings(buildSettings: _*)
  .settings(libraryDependencies ++= Seq(
    Dependencies.atlasAkka,
    Dependencies.atlasCore,
    Dependencies.atlasJson,
    Dependencies.atlasModuleAkka,
    Dependencies.awsAutoScaling,
    Dependencies.awsCore,
    Dependencies.awsEC2,
    Dependencies.awsELB,
    Dependencies.awsELBv2,
    Dependencies.iepGuice,
    Dependencies.iepModuleAdmin,
    Dependencies.iepModuleAws,
    Dependencies.iepNflxenv,
    Dependencies.iepService,
    Dependencies.jacksonCore2,
    Dependencies.jacksonMapper2,
    Dependencies.log4jApi,
    Dependencies.log4jCore,
    Dependencies.log4jSlf4j,
    Dependencies.slf4jApi,
    Dependencies.spectatorApi,
    Dependencies.spectatorAws,
    Dependencies.typesafeConfig,
    Dependencies.scalatest % "test"
  ))

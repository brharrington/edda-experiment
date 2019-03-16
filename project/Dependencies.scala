import sbt._

object Dependencies {
  object Versions {
    val atlas      = "1.6.0-rc.14"
    val aws        = "2.4.16"
    val iep        = "2.0.0"
    val jackson    = "2.9.8"
    val log4j      = "2.11.2"
    val scala      = "2.12.8"
    val slf4j      = "1.7.26"
    val spectator  = "0.87.0"
  }

  import Versions._

  val atlasAkka       = "com.netflix.atlas_v1" %% "atlas-akka" % atlas
  val atlasCore       = "com.netflix.atlas_v1" %% "atlas-core" % atlas
  val atlasJson       = "com.netflix.atlas_v1" %% "atlas-json" % atlas
  val atlasModuleAkka = "com.netflix.atlas_v1" %% "atlas-module-akka" % atlas
  val awsAutoScaling  = "software.amazon.awssdk" % "autoscaling" % aws
  val awsCore         = "software.amazon.awssdk" % "core" % aws
  val awsEC2          = "software.amazon.awssdk" % "ec2" % aws
  val awsELB          = "software.amazon.awssdk" % "elasticloadbalancing" % aws
  val awsELBv2        = "software.amazon.awssdk" % "elasticloadbalancingv2" % aws
  val iepGuice        = "com.netflix.iep" % "iep-guice" % iep
  val iepModuleAdmin  = "com.netflix.iep" % "iep-module-admin" % iep
  val iepModuleAws    = "com.netflix.iep" % "iep-module-aws2" % iep
  val iepNflxenv      = "com.netflix.iep" % "iep-nflxenv" % iep
  val iepService      = "com.netflix.iep" % "iep-service" % iep
  val jacksonCore2    = "com.fasterxml.jackson.core" % "jackson-core" % jackson
  val jacksonMapper2  = "com.fasterxml.jackson.core" % "jackson-databind" % jackson
  val log4jApi        = "org.apache.logging.log4j" % "log4j-api" % log4j
  val log4jCore       = "org.apache.logging.log4j" % "log4j-core" % log4j
  val log4jSlf4j      = "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4j
  val scalaCompiler   = "org.scala-lang" % "scala-compiler" % scala
  val scalaLibrary    = "org.scala-lang" % "scala-library" % scala
  val scalaReflect    = "org.scala-lang" % "scala-reflect" % scala
  val scalatest       = "org.scalatest" %% "scalatest" % "3.0.6"
  val slf4jApi        = "org.slf4j" % "slf4j-api" % slf4j
  val spectatorApi    = "com.netflix.spectator" % "spectator-api" % spectator
  val spectatorAws    = "com.netflix.spectator" % "spectator-ext-aws2" % spectator
  val typesafeConfig  = "com.typesafe" % "config" % "1.3.3"
}

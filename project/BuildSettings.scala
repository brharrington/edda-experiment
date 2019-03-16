import sbt._
import sbt.Keys._

object BuildSettings {
  val organization = "com.netflix.edda"

  val compilerFlags = Seq(
    "-deprecation",
    "-unchecked",
    "-Xexperimental",
    "-Xlint:_,-infer-any",
    "-feature",
    "-target:jvm-1.8")

  val resolvers = Seq(
    Resolver.mavenLocal,
    Resolver.jcenterRepo,
    "sonatype" at "https://oss.sonatype.org/content/repositories/snapshots/",
    "jfrog" at "http://oss.jfrog.org/oss-snapshot-local")
}

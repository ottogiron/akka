/*
 * Copyright (C) 2009-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka

import sbt._
import sbt.Keys._
import java.io.File
import sbtwhitesource.WhiteSourcePlugin.autoImport.whitesourceIgnore

object Publish extends AutoPlugin {

  val defaultPublishTo = settingKey[File]("Default publish directory")

  override def trigger = allRequirements

  override lazy val projectSettings = Seq(
    pomExtra := akkaPomExtra,
    publishTo := Some(akkaPublishTo.value),
    credentials ++= akkaCredentials,
    organizationName := "Lightbend Inc.",
    organizationHomepage := Some(url("https://www.lightbend.com")),
    publishMavenStyle := true,
    pomIncludeRepository := { x =>
      false
    },
    defaultPublishTo := target.value / "repository")

  def akkaPomExtra = {
    <inceptionYear>2009</inceptionYear>
    <developers>
      <developer>
        <id>akka-contributors</id>
        <name>Akka Contributors</name>
        <email>akka-dev@googlegroups.com</email>
        <url>https://github.com/akka/akka/graphs/contributors</url>
      </developer>
    </developers>
  }

  private def akkaPublishTo = Def.setting {
    val key = new java.io.File(
      Option(System.getProperty("akka.gustav.key")).getOrElse(System.getProperty("user.home") + "/.ssh/id_rsa_gustav.pem"))
    if (isSnapshot.value)
      Resolver.sftp("Akka snapshots", "gustav.akka.io", "/home/akkarepo/www/snapshots").as("akkarepo", key)
    else
      Opts.resolver.sonatypeStaging
  }

  private def akkaCredentials: Seq[Credentials] =
    Option(System.getProperty("akka.publish.credentials")).map(f => Credentials(new File(f))).toSeq
}

/**
 * For projects that are not to be published.
 */
object NoPublish extends AutoPlugin {
  override def requires = plugins.JvmPlugin

  override def projectSettings =
    Seq(skip in publish := true, sources in (Compile, doc) := Seq.empty, whitesourceIgnore := true)
}

object DeployRsync extends AutoPlugin {
  import scala.sys.process._
  import sbt.complete.DefaultParsers._

  override def requires = plugins.JvmPlugin

  trait Keys {
    val deployRsyncArtifacts = taskKey[Seq[(File, String)]]("File or directory and a path to deploy to")
    val deployRsync = inputKey[Unit]("Deploy using rsync")
  }

  object autoImport extends Keys
  import autoImport._

  override def projectSettings = Seq(
    deployRsyncArtifacts := List(),
    deployRsync := {
      val (_, host) = (Space ~ StringBasic).parsed
      deployRsyncArtifacts.value.foreach {
        case (from, to) => s"rsync -rvz $from/ $host:$to" !
      }
    })
}

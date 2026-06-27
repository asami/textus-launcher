import org.goldenport.cozy.CozyPlugin.autoImport._

ThisBuild / organization := "org.goldenport"
ThisBuild / version := "0.1.4-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.7"
ThisBuild / publishMavenStyle := true

libraryDependencies ++= Seq(
  "org.goldenport" %% "goldenport-launcher-core" % "0.1.0-SNAPSHOT",
  "org.scalatest" %% "scalatest" % "3.2.19" % Test
)

cozyCoursierChannelPath := "repository/textus/coursier-channel.json"

cozyCoursierChannelEntries := Seq(CozyCoursierChannelEntry(
  name = "textus",
  repositories = Seq("central", "https://www.simplemodeling.org/repository/maven"),
  dependencies = Seq(s"org.goldenport:textus-launcher_3:${version.value}"),
  mainClass = "textus.launcher.TextusMain"
))

def launcherBuildInfoSource(target: File, packageName: String, launcherName: String, launcherVersion: String): File = {
  val file = target / "LauncherBuildInfo.scala"
  IO.write(file,
    s"""package $packageName
       |
       |object LauncherBuildInfo {
       |  val name: String = "$launcherName"
       |  val version: String = "$launcherVersion"
       |}
       |""".stripMargin)
  file
}

lazy val root = (project in file("."))
  .enablePlugins(org.goldenport.cozy.CozyPlugin)
  .settings(
    name := "textus-launcher",
    Compile / sourceGenerators += Def.task {
      Seq(launcherBuildInfoSource(
        (Compile / sourceManaged).value / "launcher-build-info",
        "textus.launcher",
        "textus",
        version.value
      ))
    }.taskValue,
    Compile / mainClass := Some("textus.launcher.TextusMain"),
    publishTo := {
      val repo = sys.env.get("SIMPLEMODELING_MAVEN_LOCAL")
        .map(file)
        .getOrElse(baseDirectory.value / "maven-local")
      Some(Resolver.file("local-simplemodeling-maven", repo))
    },
    Compile / packageDoc / publishArtifact := false,
    publish / packagedArtifacts := {
      cozyPublishCoursierChannel.value
      (publish / packagedArtifacts).value
    }
  )

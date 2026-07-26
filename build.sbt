import org.goldenport.cozy.CozyPlugin.autoImport._

ThisBuild / organization := "org.goldenport"
ThisBuild / version := "0.2.2"
ThisBuild / scalaVersion := "3.3.8"
ThisBuild / publishMavenStyle := true

resolvers += "SimpleModeling.org" at "https://www.simplemodeling.org/repository/maven"

libraryDependencies ++= Seq(
  "org.goldenport" %% "goldenport-launcher-core" % "0.1.0",
  "io.circe" %% "circe-core" % "0.14.6",
  "io.circe" %% "circe-generic" % "0.14.6",
  "io.circe" %% "circe-parser" % "0.14.6",
  "org.scalatest" %% "scalatest" % "3.2.19" % Test
)

cozyCoursierChannelPath := "repository/textus/coursier-channel.json"

lazy val textusExportLauncherClasspath = taskKey[File]("Write the Textus launcher development classpath")

cozyCoursierChannelEntries := Seq(CozyCoursierChannelEntry(
  name = "textus",
  repositories = Seq("central", "https://www.simplemodeling.org/repository/maven"),
  dependencies = Seq(s"org.goldenport:textus-launcher_3:${version.value}"),
  mainClass = "textus.launcher.TextusMain"
))

def launcherBuildInfoSource(target: File, packagename: String, launchername: String, launcherversion: String): File = {
  val file = target / "LauncherBuildInfo.scala"
  IO.write(file,
    s"""package $packagename
       |
       |object LauncherBuildInfo {
       |  val name: String = "$launchername"
       |  val version: String = "$launcherversion"
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
    textusExportLauncherClasspath := {
      val file = (Compile / target).value / "textus.d" / "runtime-classpath.txt"
      IO.createDirectory(file.getParentFile)
      val classpath = (Runtime / fullClasspath).value.map(_.data.getAbsolutePath).mkString(java.io.File.pathSeparator)
      IO.write(file, classpath + "\n")
      file
    },
    Test / test := {
      (Test / runMain).toTask(" textus.launcher.TextusLauncherSpec").value
    },
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

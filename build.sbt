ThisBuild / organization := "org.goldenport"
ThisBuild / version := "0.1.1-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.7"
ThisBuild / publishMavenStyle := true

lazy val publishTextusCoursierChannel = taskKey[File]("Publish the Textus Coursier channel descriptor into the warehouse repository.")

def textusPublishRepositoryFile(resolver: Resolver): Option[File] =
  resolver match {
    case m: MavenRepository =>
      val root = m.root
      if (root.startsWith("file:"))
        Some(new File(new java.net.URI(root)))
      else
        Some(file(root))
    case f: FileRepository =>
      f.patterns.artifactPatterns.headOption.flatMap { pattern =>
        val marker = "/[organisation]/"
        val index = pattern.indexOf(marker)
        if (index >= 0)
          Some(file(pattern.take(index)))
        else
          None
      }
    case _ =>
      None
  }

def textusWarehouseDirFromMavenRepository(repository: File): File =
  repository.getCanonicalFile match {
    case canonical
        if canonical.getName == "maven" &&
          canonical.getParentFile != null &&
          canonical.getParentFile.getName == "repository" =>
      canonical.getParentFile.getParentFile
    case canonical if canonical.getName == "maven" =>
      canonical.getParentFile
    case canonical =>
      sys.error(
        s"Textus Coursier channel publish requires publishTo to point at a Maven repository " +
          s"under a warehouse, but got: ${canonical}"
      )
  }

def textusCoursierChannelEntryJson(version: String): String =
  s"""{
     |  "repositories": [
     |    "central",
     |    "https://www.simplemodeling.org/repository/maven"
     |  ],
     |  "dependencies": [
     |    "org.goldenport:textus_3:$version"
     |  ],
     |  "mainClass": "textus.launcher.TextusMain"
     |}""".stripMargin

def textusParseCoursierChannelEntries(text: String): Vector[(String, String)] = {
  val source = text.trim
  if (source.isEmpty)
    Vector.empty
  else {
    val start = source.indexOf('{')
    val end = source.lastIndexOf('}')
    if (start < 0 || end <= start)
      Vector.empty
    else {
      var i = start + 1
      val entries = Vector.newBuilder[(String, String)]
      def skipWhitespaceAndCommas(): Unit =
        while (i < end && (source.charAt(i).isWhitespace || source.charAt(i) == ',')) i += 1
      while (i < end) {
        skipWhitespaceAndCommas()
        if (i < end && source.charAt(i) == '"') {
          val keyStart = i + 1
          val keyEnd = source.indexOf('"', keyStart)
          if (keyEnd < 0) {
            i = end
          } else {
            val key = source.substring(keyStart, keyEnd)
            i = keyEnd + 1
            while (i < end && (source.charAt(i).isWhitespace || source.charAt(i) == ':')) i += 1
            if (i < end && source.charAt(i) == '{') {
              val valueStart = i
              var depth = 0
              var done = false
              while (i < source.length && !done) {
                source.charAt(i) match {
                  case '{' => depth += 1
                  case '}' =>
                    depth -= 1
                    if (depth == 0) done = true
                  case _ => ()
                }
                i += 1
              }
              entries += key -> source.substring(valueStart, i)
            } else {
              i = end
            }
          }
        } else {
          i += 1
        }
      }
      entries.result()
    }
  }
}

def textusCoursierChannelJson(
  version: String,
  existing: Option[String]
): String = {
  val entries =
    existing.toVector.flatMap(textusParseCoursierChannelEntries).filterNot(_._1 == "textus") :+
      ("textus" -> textusCoursierChannelEntryJson(version))
  val rendered = entries.map { case (name, value) =>
    val lines = value.linesIterator.toVector
    val head = lines.headOption.getOrElse("{}")
    val tail = lines.drop(1).map(line => s"  $line")
    (s"""  "$name": $head""" +: tail).mkString("\n")
  }
  (Vector("{") ++ rendered.zipWithIndex.map { case (entry, index) =>
    if (index + 1 == rendered.length) entry else s"$entry,"
  } ++ Vector("}")).mkString("\n") + "\n"
}

def textusPublishCoursierChannelFile(
  version: String,
  publishResolver: Option[Resolver],
  baseDir: File,
  log: sbt.util.Logger
): File = {
  val warehouseDir =
    publishResolver
      .flatMap(textusPublishRepositoryFile)
      .map(textusWarehouseDirFromMavenRepository)
      .getOrElse(textusWarehouseDirFromMavenRepository(baseDir / "maven-local"))
  val target = warehouseDir / "repository" / "textus" / "coursier-channel.json"
  val existing =
    if (target.isFile)
      Some(IO.read(target))
    else
      None
  IO.createDirectory(target.getParentFile)
  IO.write(target, textusCoursierChannelJson(version, existing))
  log.info(s"Published Textus Coursier channel to ${target}")
  target
}

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
  .settings(
    name := "textus",
    Compile / sourceGenerators += Def.task {
      Seq(launcherBuildInfoSource(
        (Compile / sourceManaged).value / "launcher-build-info",
        "textus.launcher",
        name.value,
        version.value
      ))
    }.taskValue,
    Compile / mainClass := Some("textus.launcher.TextusMain"),
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
    publishTextusCoursierChannel := {
      textusPublishCoursierChannelFile(version.value, publishTo.value, baseDirectory.value, streams.value.log)
    },
    publish / packagedArtifacts := {
      publishTextusCoursierChannel.value
      (publish / packagedArtifacts).value
    }
  )

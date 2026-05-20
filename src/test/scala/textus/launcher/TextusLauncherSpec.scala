package textus.launcher

import java.nio.file.{Files, Path}

/*
 * @since   May. 17, 2026
 * @version May. 20, 2026
 * @author  ASAMI, Tomoharu
 */
object TextusLauncherSpec {
  def main(args: Array[String]): Unit = {
    val spec = new TextusLauncherSpec
    spec.parser()
    spec.launcherVersion()
    spec.configMerge()
    spec.runtimeVersionPrecedence()
    spec.runtimeUseWritesExpectedFiles()
    spec.runtimeUseAutoSelectsProjectWhenTextusDirectoryExists()
    spec.runtimeCatalogParseAndSelectorResolution()
    spec.runtimeCatalogCommands()
    spec.executionRewritesToCncfArgs()
    spec.runtimeCommandDoesNotLoadCncf()
    spec.latestRuntimeIsConcrete()
    spec.noRuntimeLibraryDependencies()
    println("TextusLauncherSpec: OK")
  }
}

final class TextusLauncherSpec {
  def parser(): Unit = {
    val command = TextusCommandParser.parse(Vector("command", "textus-blog:0.1.0", "blog.post.search", "limit=10"))
      .asInstanceOf[TextusCommand.Execute]
    _assert_equals(command.mode, "command")
    _assert_equals(command.artifact.name, "textus-blog")
    _assert_equals(command.artifact.version, Some("0.1.0"))
    _assert_equals(command.artifact.kind, ArtifactKind.Auto)
    _assert_equals(command.args, Vector("blog.post.search", "limit=10"))

    val legacy = TextusCommandParser.parse(Vector("server", "textus-blog@0.1.0"))
      .asInstanceOf[TextusCommand.Execute]
    _assert_equals(legacy.artifact.name, "textus-blog")
    _assert_equals(legacy.artifact.version, Some("0.1.0"))

    val forcedversion = TextusCommandParser.parse(Vector("server", "--car", "textus-blog:0.1.0"))
      .asInstanceOf[TextusCommand.Execute]
    _assert_equals(forcedversion.artifact.kind, ArtifactKind.Car)
    _assert_equals(forcedversion.artifact.version, Some("0.1.0"))

    val extversion = TextusCommandParser.parse(Vector("server", "textus-blog.car:0.1.0"))
      .asInstanceOf[TextusCommand.Execute]
    _assert_equals(extversion.artifact.name, "textus-blog")
    _assert_equals(extversion.artifact.kind, ArtifactKind.Car)
    _assert_equals(extversion.artifact.version, Some("0.1.0"))

    val mixed =
      try {
        TextusCommandParser.parse(Vector("server", "textus-blog:0.1.0@other"))
        false
      } catch {
        case e: TextusException => e.getMessage.contains("both")
      }
    assert(mixed)

    val forced = TextusCommandParser.parse(Vector("server", "--sar", "my-app"))
      .asInstanceOf[TextusCommand.Execute]
    _assert_equals(forced.artifact.kind, ArtifactKind.Sar)

    val ext = TextusCommandParser.parse(Vector("server", "textus-blog.car"))
      .asInstanceOf[TextusCommand.Execute]
    _assert_equals(ext.artifact.name, "textus-blog")
    _assert_equals(ext.artifact.kind, ArtifactKind.Car)

    val autouse = TextusCommandParser.parse(Vector("runtime", "use", "latest"))
      .asInstanceOf[TextusCommand.Runtime.Use]
    _assert_equals(autouse.version, "latest")
    _assert_equals(autouse.target, TextusCommand.RuntimeUseTarget.Auto)
  }

  def launcherVersion(): Unit = _with_temp_paths { paths =>
    val launcher = new TextusLauncher(paths, FakeResolver(), FakeInvoker())
    val (code, output) = _capture_stdout {
      launcher.run(Vector("--version"))
    }
    _assert_equals(code, 0)
    _assert_equals(output.trim, s"${LauncherBuildInfo.name} ${LauncherBuildInfo.version}")
    _assert_equals(TextusCommandParser.parse(Vector("version")), TextusCommand.Version)
    _assert_equals(TextusCommandParser.parse(Vector("launcher", "version")), TextusCommand.Version)
  }

  def configMerge(): Unit = _with_temp_paths { paths =>
    _write(paths.textusHome.resolve("config.yaml"),
      """runtime:
        |  version: 0.1.0
        |  catalog:
        |    url: https://global.example/catalog.yaml
        |repositories:
        |  car:
        |    - https://global.example/car
        |  sar:
        |    - https://global.example/sar
        |""".stripMargin)
    _write(paths.cwd.resolve(".textus").resolve("config.yaml"),
      """runtime:
        |  version: 0.2.0
        |repositories:
        |  car:
        |    - https://project.example/car
        |""".stripMargin)
    val config = LauncherConfig.load(paths)
    _assert_equals(config.runtimeVersion, Some("0.2.0"))
    _assert_equals(config.runtimeCatalogUrl, Some("https://global.example/catalog.yaml"))
    assert(config.carRepositories.head == "https://project.example/car")
    assert(config.carRepositories(1) == "https://global.example/car")
    assert(config.sarRepositories.head == "https://global.example/sar")
    assert(config.carRepositories.contains("https://www.simplemodeling.org/repository/car"))
  }

  def runtimeVersionPrecedence(): Unit = _with_temp_paths { paths =>
    val store = RuntimeVersionStore(paths)
    val config = LauncherConfig(runtimeVersion = Some("0.1.0"))
    _assert_equals(store.current(None, config), "0.1.0")
    store.useGlobal("0.2.0")
    _assert_equals(store.current(None, config), "0.2.0")
    store.useProject("0.3.0")
    _assert_equals(store.current(None, config), "0.3.0")
    _assert_equals(store.current(Some("0.4.0"), config), "0.4.0")
  }

  def runtimeUseWritesExpectedFiles(): Unit = _with_temp_paths { paths =>
    val launcher = new TextusLauncher(paths, FakeResolver(), FakeInvoker())
    launcher.run(Vector("runtime", "use", "latest"))
    _assert_equals(Files.readString(paths.globalVersion).trim, "latest")
    launcher.run(Vector("runtime", "use", "0.2.0", "--global"))
    launcher.run(Vector("runtime", "use", "0.3.0", "--project"))
    _assert_equals(Files.readString(paths.globalVersion).trim, "0.2.0")
    _assert_equals(Files.readString(paths.projectVersion).trim, "0.3.0")
  }

  def runtimeUseAutoSelectsProjectWhenTextusDirectoryExists(): Unit = _with_temp_paths { paths =>
    Files.createDirectories(paths.cwd.resolve(".textus"))
    val launcher = new TextusLauncher(paths, FakeResolver(), FakeInvoker())
    launcher.run(Vector("runtime", "use", "latest"))
    _assert_equals(Files.readString(paths.projectVersion).trim, "latest")
    assert(!Files.isRegularFile(paths.globalVersion))
  }

  def runtimeCatalogParseAndSelectorResolution(): Unit = {
    val catalog = RuntimeCatalog.parse(_catalog_text)
    _assert_equals(catalog.resolve("recommended").version, "0.2.0")
    _assert_equals(catalog.resolve("latest").version, "0.2.0")
    _assert_equals(catalog.resolve("latest-stable").version, "0.2.0")
    _assert_equals(catalog.resolve("latest-snapshot").version, "0.3.0-SNAPSHOT")
    _assert_equals(catalog.resolve("newest").version, "0.3.0-SNAPSHOT")
    val disabled =
      try {
        catalog.resolve("0.1.0")
        false
      } catch {
        case e: TextusException => e.getMessage.contains("disabled")
      }
    assert(disabled)
  }

  def runtimeCatalogCommands(): Unit = _with_temp_paths { paths =>
    val catalogfile = paths.cwd.resolve("runtime-catalog.yaml")
    _write(catalogfile, _catalog_text)
    _write(paths.cwd.resolve(".textus").resolve("config.yaml"),
      s"""runtime:
         |  catalog:
         |    url: $catalogfile
         |""".stripMargin)
    val launcher = new TextusLauncher(paths, CoursierCncfRuntimeResolver("false"), FakeInvoker())
    launcher.run(Vector("runtime", "refresh"))
    assert(Files.isRegularFile(paths.runtimeCatalog))
    launcher.run(Vector("runtime", "remote", "list"))
    launcher.run(Vector("runtime", "catalog", "show"))
    launcher.run(Vector("runtime", "channels"))
    launcher.run(Vector("runtime", "use", "recommended", "--project"))
    _assert_equals(Files.readString(paths.projectVersion).trim, "recommended")
    launcher.run(Vector("runtime", "current"))
  }

  def executionRewritesToCncfArgs(): Unit = _with_temp_paths { paths =>
    val carrepo = paths.cwd.resolve("repo").resolve("car")
    _write(carrepo.resolve("textus-blog").resolve("0.1.0").resolve("textus-blog-0.1.0.car"), "")
    _write(paths.cwd.resolve(".textus").resolve("config.yaml"),
      s"""runtime:
         |  version: 0.5.0
         |  catalog:
         |    url: ${paths.cwd.resolve("missing-runtime-catalog.yaml")}
         |repositories:
         |  car:
         |    - ${carrepo}
         |""".stripMargin)
    val invoker = FakeInvoker()
    val launcher = new TextusLauncher(paths, FakeResolver(), invoker)
    launcher.run(Vector("command", "--car", "textus-blog:0.1.0", "blog.post.search", "limit=10"))
    _assert_equals(invoker.lastArgs, Vector(
      s"--repository-dir=$carrepo",
      "--repository-dir=https://www.simplemodeling.org/repository/car",
      "--repository-dir=https://www.simplemodeling.org/repository/sar",
      "--textus.component=textus-blog",
      "--textus.component.version=0.1.0",
      "command",
      "blog.post.search",
      "limit=10"
    ))
  }

  def runtimeCommandDoesNotLoadCncf(): Unit = _with_temp_paths { paths =>
    _write(paths.cwd.resolve(".textus").resolve("config.yaml"), "runtime:\n  version: 0.5.0\n")
    val resolver = FakeResolver()
    val invoker = FakeInvoker()
    val launcher = new TextusLauncher(paths, resolver, invoker)
    launcher.run(Vector("runtime", "current"))
    _assert_equals(resolver.resolvedClasspaths, Vector.empty)
    _assert_equals(invoker.lastArgs, Vector.empty)
  }

  def latestRuntimeIsConcrete(): Unit = _with_temp_paths { paths =>
    val resolver = FakeResolver()
    val launcher = new TextusLauncher(paths, resolver, FakeInvoker())
    launcher.run(Vector("runtime", "current"))
    _assert_equals(resolver.resolvedVersions, Vector(LauncherConfig.DEFAULT_RUNTIME_VERSION))
  }

  def noRuntimeLibraryDependencies(): Unit = {
    val build = Files.readString(Path.of("build.sbt"))
    assert(!build.contains("libraryDependencies +="))
    assert(!build.contains("libraryDependencies ++="))
  }

  private def _capture_stdout(f: => Int): (Int, String) = {
    val out = new java.io.ByteArrayOutputStream()
    val code = Console.withOut(new java.io.PrintStream(out)) {
      f
    }
    (code, out.toString)
  }

  private def _with_temp_paths(f: LauncherPaths => Unit): Unit = {
    val root = Files.createTempDirectory("textus-launcher-spec-")
    val home = root.resolve("home")
    val cwd = root.resolve("work")
    Files.createDirectories(home)
    Files.createDirectories(cwd)
    f(LauncherPaths(home, cwd))
  }

  private def _write(path: Path, value: String): Unit = {
    Files.createDirectories(path.getParent)
    Files.writeString(path, value)
  }

  private def _assert_equals[A](actual: A, expected: A): Unit =
    assert(actual == expected, s"expected=$expected actual=$actual")

  private val _catalog_text: String =
    """schemaVersion: 1
      |generatedAt: 2026-05-17T00:00:00Z
      |recommended: 0.2.0
      |latestStable: 0.2.0
      |latestSnapshot: 0.3.0-SNAPSHOT
      |mavenRepositories:
      |  - https://repo.example/maven
      |carRepositories:
      |  - https://repo.example/car
      |sarRepositories:
      |  - https://repo.example/sar
      |coursierRepositories:
      |  - https://repo.example/coursier
      |versions:
      |  - version: 0.1.0
      |    channel: stable
      |    status: disabled
      |    scalaBinaryVersion: "3"
      |    module: org.goldenport:goldenport-cncf_3:0.1.0
      |  - version: 0.2.0
      |    channel: stable
      |    status: active
      |    scalaBinaryVersion: "3"
      |    module: org.goldenport:goldenport-cncf_3:0.2.0
      |    publishedAt: 2026-05-17T01:00:00Z
      |  - version: 0.3.0-SNAPSHOT
      |    channel: snapshot
      |    status: active
      |    scalaBinaryVersion: "3"
      |    module: org.goldenport:goldenport-cncf_3:0.3.0-SNAPSHOT
      |    publishedAt: 2026-05-17T02:00:00Z
      |""".stripMargin
}

final class FakeResolver extends CncfRuntimeResolver {
  var resolvedVersions: Vector[String] = Vector.empty
  var resolvedClasspaths: Vector[String] = Vector.empty
  override def resolveVersion(version: String, config: LauncherConfig, paths: LauncherPaths): String = {
    resolvedVersions :+= version
    if (version == LauncherConfig.DEFAULT_RUNTIME_VERSION) "0.9.0" else version
  }
  def resolve(version: String, config: LauncherConfig, paths: LauncherPaths): Vector[Path] = {
    val concreteversion = resolveVersion(version, config, paths)
    resolvedClasspaths :+= concreteversion
    Vector(paths.cwd.resolve(s"fake-cncf-$concreteversion.jar"))
  }
}

object FakeResolver {
  def apply(): FakeResolver = new FakeResolver()
}

final class FakeInvoker extends CncfInvoker {
  var lastClasspath: Vector[Path] = Vector.empty
  var lastArgs: Vector[String] = Vector.empty
  override def invoke(classpath: Vector[Path], args: Vector[String]): Int = {
    lastClasspath = classpath
    lastArgs = args
    0
  }
}

object FakeInvoker {
  def apply(): FakeInvoker = new FakeInvoker()
}

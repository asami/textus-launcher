package textus.launcher

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import java.net.InetSocketAddress
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

/*
 * @since   May. 17, 2026
 *  version May. 27, 2026
 *  version Jun. 29, 2026
 * @version Jul. 18, 2026
 * @author  ASAMI, Tomoharu
 */
object TextusLauncherSpec {
  def main(args: Array[String]): Unit = {
    val spec = new TextusLauncherSpec
    spec.parser()
    spec.helpExplainsLocalRepository()
    spec.runtimeVersion()
    spec.runtimeDevelopmentConfig()
    spec.launcherVersion()
    spec.runtimeHelp()
    spec.configMerge()
    spec.workspaceRootConfigAppliesToNestedCwd()
    spec.launcherConfigSupportsPropertiesAndConfFiles()
    spec.launcherDevDirDelegatesToDevelopmentLauncher()
    spec.launcherDevDirRejectsStaleDevelopmentClasspath()
    spec.runtimeVersionPrecedence()
    spec.runtimeUseWritesExpectedFiles()
    spec.runtimeUseAutoSelectsProjectWhenTextusDirectoryExists()
    spec.installCliWritesUserFacingCommand()
    spec.installCliFallsBackFromStaleLocalCatalogToRemoteCar()
    spec.missingLocalAndRemoteCatalogArchivesReportBothDiagnostics()
    spec.runtimeCatalogParseAndSelectorResolution()
    spec.runtimeCatalogCommands()
    spec.runtimeCurrentWarnsWhenCachedRecommendedIsStale()
    spec.executionRewritesToCncfArgs()
    spec.textusAdminRegistrationLifecycle()
    spec.textusAdminRegistrationHttpLifecycle()
    spec.localRepositoryResolvesArtifactWithoutConfig()
    spec.snapshotArtifactDoesNotFallThroughToCacheOrPublic()
    spec.artifactCatalogUsesCurrentCompatibleRuntimeByDefault()
    spec.artifactCatalogCanSelectLatestTestedRuntime()
    spec.artifactCatalogCanSelectLatestCompatibleRuntime()
    spec.artifactCatalogCanSelectNewestCompatibleRuntime()
    spec.artifactCatalogIncludesDependencyRequirements()
    spec.artifactCatalogDoesNotFallbackToMetadataWhenCatalogRejectsVersion()
    spec.runtimeConflictDefaultsToError()
    spec.runtimeConflictCanUseNewestPolicy()
    spec.explicitRuntimeIsValidatedAgainstArtifactRequirement()
    spec.runtimeCommandDoesNotLoadCncf()
    spec.latestRuntimeIsConcrete()
    spec.noRuntimeLibraryDependencies()
    println("TextusLauncherSpec: OK")
  }
}

final class TextusLauncherSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "textus launcher" should {
    "command parsing" which {
      "parser" in {
        Given("the textus launcher scenario: parser")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        parser()
      }

      "runtime catalog parse and selector resolution" in {
        Given("the textus launcher scenario: runtime catalog parse and selector resolution")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        runtimeCatalogParseAndSelectorResolution()
      }

    }

    "configuration and launcher metadata" which {
      "launcher version" in {
        Given("the textus launcher scenario: launcher version")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        launcherVersion()
      }

      "config merge" in {
        Given("the textus launcher scenario: config merge")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        configMerge()
      }

      "workspace root config applies to nested cwd" in {
        Given("the textus launcher scenario: workspace root config applies to nested cwd")
        When("the launcher loads config from a nested sample directory")
        Then("the executable specification holds through inherited root config")
        workspaceRootConfigAppliesToNestedCwd()
      }

      "launcher config supports properties and conf files" in {
        Given("the textus launcher scenario: launcher config supports properties and conf files")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        launcherConfigSupportsPropertiesAndConfFiles()
      }

      "launcher dev dir delegates to development launcher" in {
        Given("a global Textus launcher config selects a development launcher checkout")
        When("the launcher command is run")
        Then("the configured checkout receives the original command arguments")
        launcherDevDirDelegatesToDevelopmentLauncher()
      }

      "launcher dev dir rejects stale development classpath" in {
        Given("a Textus development launcher classpath without the Textus main class")
        When("the launcher tries to delegate")
        Then("the stale classpath is rejected before starting a process")
        launcherDevDirRejectsStaleDevelopmentClasspath()
      }

      "runtime development config" in {
        Given("a Textus launcher config declares a development CNCF runtime checkout")
        When("development mode or environment override is supplied")
        Then("the runtime development directory is activated through launcher config")
        runtimeDevelopmentConfig()
      }

      "local repository resolves artifact without config" in {
        Given("the textus launcher scenario: local repository resolves artifact without config")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        localRepositoryResolvesArtifactWithoutConfig()
      }

    }

    "runtime selection and catalog operations" which {
      "runtime version" in {
        Given("the textus launcher scenario: runtime version")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        runtimeVersion()
      }

      "runtime help" in {
        Given("the textus launcher scenario: runtime help")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        runtimeHelp()
      }

      "runtime version precedence" in {
        Given("the textus launcher scenario: runtime version precedence")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        runtimeVersionPrecedence()
      }

      "runtime use writes expected files" in {
        Given("the textus launcher scenario: runtime use writes expected files")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        runtimeUseWritesExpectedFiles()
      }

      "runtime use auto selects project when textus directory exists" in {
        Given("the textus launcher scenario: runtime use auto selects project when textus directory exists")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        runtimeUseAutoSelectsProjectWhenTextusDirectoryExists()
      }

      "install cli writes user facing command" in {
        Given("the textus launcher scenario: install cli writes user facing command")
        When("the launcher installs a domain command")
        Then("the command delegates to textus command with file parameter expansion")
        installCliWritesUserFacingCommand()
      }

      "install cli falls back from stale local catalog to remote CAR" in {
        Given("a stale local CAR catalog whose archive is missing")
        When("the launcher installs a command for a published CAR available remotely")
        Then("the command wrapper is generated from the remote CAR version")
        installCliFallsBackFromStaleLocalCatalogToRemoteCar()
      }

      "missing local and remote catalog archives report both diagnostics" in {
        Given("local and remote catalogs whose selected archives are both missing")
        When("the resolver cannot locate the requested CAR")
        Then("the error identifies both failed catalog sources")
        missingLocalAndRemoteCatalogArchivesReportBothDiagnostics()
      }

      "runtime catalog commands" in {
        Given("the textus launcher scenario: runtime catalog commands")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        runtimeCatalogCommands()
      }

      "runtime current warns when cached recommended is stale" in {
        Given("the textus launcher scenario: runtime current warns when cached recommended is stale")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        runtimeCurrentWarnsWhenCachedRecommendedIsStale()
      }

      "runtime conflict defaults to error" in {
        Given("the textus launcher scenario: runtime conflict defaults to error")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        runtimeConflictDefaultsToError()
      }

      "runtime conflict can use newest policy" in {
        Given("the textus launcher scenario: runtime conflict can use newest policy")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        runtimeConflictCanUseNewestPolicy()
      }

      "runtime command does not load cncf" in {
        Given("the textus launcher scenario: runtime command does not load cncf")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        runtimeCommandDoesNotLoadCncf()
      }

      "latest runtime is concrete" in {
        Given("the textus launcher scenario: latest runtime is concrete")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        latestRuntimeIsConcrete()
      }

    }

    "artifact execution and resolution" which {
      "execution rewrites to cncf args" in {
        Given("the textus launcher scenario: execution rewrites to cncf args")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        executionRewritesToCncfArgs()
      }

      "snapshot artifact does not fall through to cache or public" in {
        Given("the textus launcher scenario: snapshot artifact does not fall through to cache or public")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        snapshotArtifactDoesNotFallThroughToCacheOrPublic()
      }

      "artifact catalog uses current compatible runtime by default" in {
        Given("the textus launcher scenario: artifact catalog uses current compatible runtime by default")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        artifactCatalogUsesCurrentCompatibleRuntimeByDefault()
      }

      "artifact catalog can select latest tested runtime" in {
        Given("the textus launcher scenario: artifact catalog can select latest tested runtime")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        artifactCatalogCanSelectLatestTestedRuntime()
      }

      "artifact catalog can select latest compatible runtime" in {
        Given("the textus launcher scenario: artifact catalog can select latest compatible runtime")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        artifactCatalogCanSelectLatestCompatibleRuntime()
      }

      "artifact catalog can select newest compatible runtime" in {
        Given("the textus launcher scenario: artifact catalog can select newest compatible runtime")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        artifactCatalogCanSelectNewestCompatibleRuntime()
      }

      "artifact catalog includes dependency requirements" in {
        Given("the textus launcher scenario: artifact catalog includes dependency requirements")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        artifactCatalogIncludesDependencyRequirements()
      }

      "artifact catalog does not fallback to metadata when catalog rejects version" in {
        Given("the textus launcher scenario: artifact catalog does not fallback to metadata when catalog rejects version")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        artifactCatalogDoesNotFallbackToMetadataWhenCatalogRejectsVersion()
      }

      "explicit runtime is validated against artifact requirement" in {
        Given("the textus launcher scenario: explicit runtime is validated against artifact requirement")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        explicitRuntimeIsValidatedAgainstArtifactRequirement()
      }

    }

    "packaging boundaries" which {
      "no runtime library dependencies" in {
        Given("the textus launcher scenario: no runtime library dependencies")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        noRuntimeLibraryDependencies()
      }

    }

    "launcher behavior" which {
      "help explains local repository" in {
        Given("the textus launcher scenario: help explains local repository")
        When("the launcher behavior is exercised")
        Then("the executable specification holds through scenario-specific expectations")
        helpExplainsLocalRepository()
      }

    }

  }

  def parser(): Unit = {
    val command = TextusCommandParser.parse(Vector("command", "textus-blog:0.1.0", "blog.post.search", "limit=10"))
      .asInstanceOf[TextusCommand.Execute]
    _assert_equals(command.mode, "command")
    _assert_equals(command.artifact.name, "textus-blog")
    _assert_equals(command.artifact.version, Some("0.1.0"))
    _assert_equals(command.artifact.kind, ArtifactKind.Auto)
    _assert_equals(command.args, Vector("blog.post.search", "limit=10"))

    val targetfirst = TextusCommandParser.parse(Vector("textus-blog:0.1.0", "command", "blog.post.search", "limit=10"))
      .asInstanceOf[TextusCommand.Execute]
    _assert_equals(targetfirst.mode, "command")
    _assert_equals(targetfirst.artifact.name, "textus-blog")
    _assert_equals(targetfirst.artifact.version, Some("0.1.0"))
    _assert_equals(targetfirst.args, Vector("blog.post.search", "limit=10"))

    val legacy = TextusCommandParser.parse(Vector("server", "textus-blog@0.1.0"))
      .asInstanceOf[TextusCommand.Execute]
    _assert_equals(legacy.artifact.name, "textus-blog")
    _assert_equals(legacy.artifact.version, Some("0.1.0"))

    val forcedversion = TextusCommandParser.parse(Vector("server", "--car", "textus-blog:0.1.0"))
      .asInstanceOf[TextusCommand.Execute]
    _assert_equals(forcedversion.artifact.kind, ArtifactKind.Car)
    _assert_equals(forcedversion.artifact.version, Some("0.1.0"))
    _assert_equals(forcedversion.runtimeSelectionPolicy, None)

    val selection = TextusCommandParser.parse(Vector("--runtime-selection=tested-latest", "server", "textus-blog:0.1.0"))
      .asInstanceOf[TextusCommand.Execute]
    _assert_equals(selection.runtimeSelectionPolicy, Some(RuntimeSelectionPolicy.TestedLatest))
    val newestselection = TextusCommandParser.parse(Vector("--runtime-selection=newest", "server", "textus-blog:0.1.0"))
      .asInstanceOf[TextusCommand.Execute]
    _assert_equals(newestselection.runtimeSelectionPolicy, Some(RuntimeSelectionPolicy.NewestCompatible))
    val latestselection = TextusCommandParser.parse(Vector("--runtime-selection=latest", "server", "textus-blog:0.1.0"))
      .asInstanceOf[TextusCommand.Execute]
    _assert_equals(latestselection.runtimeSelectionPolicy, Some(RuntimeSelectionPolicy.LatestCompatible))

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
    mixed shouldBe true

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

    val install = TextusCommandParser.parse(Vector(
      "install-cli",
      "sanpomap",
      "textus-sanpomap:0.1.0",
      "--operation-prefix",
      "sanpomap.presentation",
      "--file-param",
      "presentationDsl",
      "--bin-dir",
      "bin",
      "--overwrite"
    )).asInstanceOf[TextusCommand.InstallCli]
    _assert_equals(install.name, "sanpomap")
    _assert_equals(install.artifact.name, "textus-sanpomap")
    _assert_equals(install.artifact.version, Some("0.1.0"))
    _assert_equals(install.operationPrefix, "sanpomap.presentation")
    _assert_equals(install.fileParams, Vector("presentationDsl"))
    _assert_equals(install.binDir, Some("bin"))
    install.overwrite shouldBe true
  }

  def helpExplainsLocalRepository(): Unit = {
    val help = TextusCommandParser.helpText
    help.contains("~/.cncf/local/repository/car") shouldBe true
    help.contains("cozyPublishLocalCar") shouldBe true
    help.contains("~/.cncf/local is developer local publish state") shouldBe true
    help.contains("Snapshot components are local-only") shouldBe true
    help.contains("yaml/yml, properties/props, and lightweight conf") shouldBe true
    help.contains("--runtime-dev-dir <dir>") shouldBe true
    help.contains("Config launcher.dev-dir delegates the installed launcher") shouldBe true
    help.contains("sbt textusExportLauncherClasspath") shouldBe true
    help.contains("runtime.dev-dir is the configuration equivalent of --runtime-dev-dir") shouldBe true
    help.contains("development.runtime.dev-dir") shouldBe true
    help.contains("TEXTUS_USE_DEVELOPMENT=true") shouldBe true
    help.contains("TEXTUS_RUNTIME_DEV_DIR") shouldBe true
    help.contains("ancestor and cwd .textus config/launcher files") shouldBe true
    help.contains("~/.textus/launcher.yaml") shouldBe true
    help.contains("Install CLI:") shouldBe true
    help.contains("--file-param <name> reads existing file values") shouldBe true
  }

  def runtimeVersion(): Unit = _with_temp_paths { paths =>
    Given("a project selects a Textus runtime version in launcher config")
    _write(paths.cwd.resolve(".textus").resolve("config.yaml"), "runtime:\n  version: 0.1.0\n")
    val invoker = FakeInvoker()
    val resolver = FakeResolver()
    val launcher = new TextusLauncher(paths, resolver, invoker)

    When("the launcher version command is executed")
    val code = launcher.run(Vector("version"))

    Then("the launcher delegates version reporting to the selected CNCF runtime")
    _assert_equals(code, 0)
    _assert_equals(resolver.resolvedVersions, Vector("0.1.0"))
    _assert_equals(resolver.resolvedClasspaths, Vector("0.1.0"))
    _assert_equals(invoker.lastClasspath, Vector(paths.cwd.resolve("fake-cncf-0.1.0.jar")))
    _assert_equals(invoker.lastArgs, Vector("version"))
    _assert_equals(TextusCommandParser.parse(Vector("version")), TextusCommand.Runtime.Version(None, None))
    _assert_equals(TextusCommandParser.parse(Vector("--version")), TextusCommand.Runtime.Version(None, None))

    Given("a runtime development directory is provided explicitly")
    val devdir = paths.cwd.resolve("cncf-dev")
    val classdir = devdir.resolve("target").resolve("classes")
    val devinvoker = FakeInvoker()
    val exporter = FakeClasspathExporter(classdir.toString)
    val devlauncher = new TextusLauncher(paths, FakeResolver(), devinvoker, exporter)

    When("the launcher version command is executed against the runtime development directory")
    val devcode = devlauncher.run(Vector("--runtime-dev-dir", devdir.toString, "version"))

    Then("the launcher invokes the development CNCF runtime version command")
    _assert_equals(devcode, 0)
    _assert_equals(exporter.projects, Vector(devdir))
    _assert_equals(devinvoker.lastClasspath, Vector(classdir))
    _assert_equals(devinvoker.lastArgs, Vector("version"))
    _assert_equals(
      TextusCommandParser.parse(Vector("--runtime-dev-dir", devdir.toString, "version")),
      TextusCommand.Runtime.Version(None, Some(devdir.toString))
    )
  }

  def runtimeDevelopmentConfig(): Unit = _with_temp_paths { paths =>
    Given("a launcher config declares an inactive development runtime directory")
    _write(paths.cwd.resolve(".textus").resolve("config.yaml"), "development:\n  runtime:\n    dev-dir: ../cncf-dev\n")

    When("the launcher config is loaded without development mode")
    val inactive = LauncherConfig.load(paths, Vector.empty, Map.empty)

    Then("the development directory remains metadata and is not active")
    _assert_equals(inactive.runtimeDevDir, None)
    _assert_equals(inactive.developmentRuntimeDevDir, Some("../cncf-dev"))

    When("TEXTUS_USE_DEVELOPMENT enables development mode")
    val active = LauncherConfig.load(paths, Vector.empty, Map("TEXTUS_USE_DEVELOPMENT" -> "true"))

    Then("the development runtime directory becomes the active runtime development directory")
    _assert_equals(active.runtimeDevDir, Some("../cncf-dev"))

    When("TEXTUS_RUNTIME_DEV_DIR is provided")
    val overridden = LauncherConfig.load(paths, Vector.empty, Map(
      "TEXTUS_USE_DEVELOPMENT" -> "true",
      "TEXTUS_RUNTIME_DEV_DIR" -> "/tmp/textus-runtime"
    ))

    Then("the explicit environment runtime development directory takes precedence")
    _assert_equals(overridden.runtimeDevDir, Some("/tmp/textus-runtime"))

    And("CNCF_RUNTIME_DEV_DIR is accepted as a CNCF-compatible alias")
    val cncfalias = LauncherConfig.load(paths, Vector.empty, Map("CNCF_RUNTIME_DEV_DIR" -> "/tmp/cncf-runtime"))
    _assert_equals(cncfalias.runtimeDevDir, Some("/tmp/cncf-runtime"))
  }

  def launcherVersion(): Unit = _with_temp_paths { paths =>
    val launcher = new TextusLauncher(paths, FakeResolver(), FakeInvoker())
    val (code, output) = _capture_stdout {
      launcher.run(Vector("launcher", "version"))
    }
    _assert_equals(code, 0)
    _assert_equals(output.trim, s"textus ${LauncherBuildInfo.version}")
    _assert_equals(TextusCommandParser.parse(Vector("launcher", "version")), TextusCommand.LauncherVersion)
    _assert_equals(TextusCommandParser.parse(Vector("launcher", "--version")), TextusCommand.LauncherVersion)
  }

  def runtimeHelp(): Unit = _with_temp_paths { paths =>
    val invoker = FakeInvoker()
    val launcher = new TextusLauncher(paths, FakeResolver(), invoker)
    val (code, output) = _capture_stdout {
      launcher.run(Vector("help"))
    }
    _assert_equals(code, 0)
    _assert_equals(invoker.lastArgs, Vector("--help"))
    output.contains("Launcher help:") shouldBe true
    output.contains("textus launcher version") shouldBe true
    output.contains("Textus launcher config loads from ~/.textus/config.yaml") shouldBe true
    _assert_equals(TextusCommandParser.parse(Vector("help")), TextusCommand.RuntimeHelp)
    _assert_equals(TextusCommandParser.parse(Vector("--help")), TextusCommand.RuntimeHelp)
    _assert_equals(TextusCommandParser.parse(Vector("launcher", "help")), TextusCommand.LauncherHelp)
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
    config.carRepositories.head == "https://project.example/car" shouldBe true
    config.carRepositories(1) == "https://global.example/car" shouldBe true
    config.sarRepositories.head == "https://global.example/sar" shouldBe true
    config.carRepositories.contains(paths.localCarRepository.toString) shouldBe true
    config.sarRepositories.contains(paths.localSarRepository.toString) shouldBe true
    config.carRepositories.contains(paths.cacheCarRepository.toString) shouldBe true
    config.sarRepositories.contains(paths.cacheSarRepository.toString) shouldBe true
    config.carRepositories.contains("https://www.simplemodeling.org/repository/car") shouldBe true
  }

  def workspaceRootConfigAppliesToNestedCwd(): Unit = _with_temp_paths { paths =>
    val workspace = paths.cwd
    val sample = workspace.resolve("samples").resolve("textus-app")
    _write(workspace.resolve(".textus").resolve("config.yaml"),
      """runtime:
        |  version: root
        |repositories:
        |  maven:
        |    - https://root.example/maven
        |""".stripMargin)
    _write(sample.resolve(".textus").resolve("config.yaml"),
      """runtime:
        |  version: sample
        |""".stripMargin)

    val config = LauncherConfig.load(paths.withCwd(sample))

    _assert_equals(config.runtimeVersion, Some("sample"))
    config.mavenRepositories.contains("https://root.example/maven") shouldBe true
  }

  def launcherConfigSupportsPropertiesAndConfFiles(): Unit = _with_temp_paths { paths =>
    _write(paths.cwd.resolve("etc").resolve("launcher.properties"),
      """runtime.version = 0.2.0
        |repositories.car = https://properties.example/car
        |""".stripMargin)
    _write(paths.cwd.resolve("etc").resolve("launcher.conf"),
      """runtime.cncf.selection-policy = latest
        |runtime.cncf.no-compatible-policy = newest
        |repositories.maven: https://conf.example/maven
        |""".stripMargin)
    val config = LauncherConfig.load(paths, Vector("etc/launcher.properties", "etc/launcher.conf"))
    _assert_equals(config.runtimeVersion, Some("0.2.0"))
    _assert_equals(config.runtimeSelectionPolicy, Some(RuntimeSelectionPolicy.LatestCompatible))
    _assert_equals(config.runtimeNoCompatiblePolicy, Some(RuntimeNoCompatiblePolicy.Newest))
    config.carRepositories.head == "https://properties.example/car" shouldBe true
    config.mavenRepositories.head == "https://conf.example/maven" shouldBe true

    val resolver = FakeResolver()
    val launcher = new TextusLauncher(paths, resolver, FakeInvoker())
    launcher.run(Vector("--config", "etc/launcher.properties", "runtime", "current"))
    _assert_equals(resolver.resolvedVersions, Vector("0.2.0"))
  }

  def launcherDevDirDelegatesToDevelopmentLauncher(): Unit = _with_temp_paths { paths =>
    val launcherdevdir = paths.cwd.resolve("launcher-textus")
    Files.createDirectories(launcherdevdir)
    _write(paths.textusHome.resolve("launcher.yaml"),
      s"""textus:
         |  launcher:
         |    dev:
         |      dir: $launcherdevdir
         |""".stripMargin)
    val invoker = FakeTextusLauncherDevInvoker()
    val launcher = new TextusLauncher(paths, FakeResolver(), FakeInvoker(), SbtRuntimeClasspathExporter, invoker)

    val code = launcher.run(Vector("launcher", "version"))

    _assert_equals(code, 0)
    _assert_equals(invoker.devDir, Some(launcherdevdir.toAbsolutePath.normalize))
    _assert_equals(invoker.args, Vector("launcher", "version"))
    _assert_equals(invoker.cwd, Some(paths.cwd.toAbsolutePath.normalize))
  }

  def launcherDevDirRejectsStaleDevelopmentClasspath(): Unit = _with_temp_paths { paths =>
    val launcherdevdir = paths.cwd.resolve("launcher-textus")
    val staleclassdir = paths.cwd.resolve("stale-textus-runtime-classes")
    Files.createDirectories(launcherdevdir)
    Files.createDirectories(staleclassdir)
    _write(launcherdevdir.resolve("target").resolve("textus.d").resolve("runtime-classpath.txt"), staleclassdir.toString)

    val e = intercept[TextusException] {
      TextusLauncherDevInvoker.System.invoke(launcherdevdir, Vector("launcher", "version"), paths.cwd)
    }

    e.getMessage.contains("does not contain textus.launcher.TextusMain") shouldBe true
    e.getMessage.contains("run sbt textusExportLauncherClasspath") shouldBe true
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
    Files.isRegularFile(paths.globalVersion) shouldBe false
  }

  def installCliWritesUserFacingCommand(): Unit = _with_temp_paths { paths =>
    val bindir = paths.cwd.resolve("bin")
    _write(paths.cwd.resolve(".textus").resolve("config.yaml"), "runtime:\n  version: 0.4.12\nrepositories:\n  car:\n    - relative-car\n")
    _write(paths.cwd.resolve("relative-car").resolve("textus-sanpomap").resolve("0.1.0").resolve("textus-sanpomap-0.1.0.car"), "car")
    val launcher = new TextusLauncher(paths, FakeResolver(), FakeInvoker())
    val code = launcher.run(Vector(
      "install-cli",
      "sanpomap",
      "textus-sanpomap:0.1.0",
      "--operation-prefix",
      "sanpomap.presentation",
      "--file-param",
      "presentationDsl",
      "--bin-dir",
      bindir.toString
    ))

    _assert_equals(code, 0)
    val script = Files.readString(bindir.resolve("sanpomap"))
    val config = Files.readString(bindir.resolve("sanpomap.config.yaml"))
    script.contains("artifact='textus-sanpomap:0.1.0'") shouldBe true
    script.contains(s"launcher_config='${bindir.resolve("sanpomap.config.yaml").toAbsolutePath.normalize}'") shouldBe true
    script.contains("operation_prefix='sanpomap.presentation'") shouldBe true
    script.contains("runtime_version='0.4.12'") shouldBe true
    script.contains("runtime_dev_dir=''") shouldBe true
    config.contains(s"    - ${paths.cwd.resolve("relative-car").toAbsolutePath.normalize}") shouldBe true
    config.contains(s"    - ${paths.localCarRepository}") shouldBe true
    script.contains("presentationDsl") shouldBe true
    script.contains("presentation-dsl") shouldBe true
    script.contains("exec textus --config \"$launcher_config\" \"${textus_args[@]}\" \"$artifact\" command \"$selector\"") shouldBe true
    Files.isExecutable(bindir.resolve("sanpomap")) shouldBe true
  }

  def installCliFallsBackFromStaleLocalCatalogToRemoteCar(): Unit = _with_temp_paths { paths =>
    val bindir = paths.cwd.resolve("bin")
    _write(paths.localRepository.resolve("repository").resolve("catalog").resolve("car").resolve("textus-sanpomap.yaml"),
      _sanpomap_catalog_text("0.1.0"))
    _with_http_repository(Map(
      "/repository/catalog/car/textus-sanpomap.yaml" -> _sanpomap_catalog_text("0.2.0"),
      "/repository/car/textus-sanpomap/0.2.0/textus-sanpomap-0.2.0.car" -> "car"
    )) { repository =>
      _write(paths.cwd.resolve("runtime-catalog.yaml"), _catalog_text)
      _write(paths.cwd.resolve(".textus").resolve("config.yaml"),
        s"""runtime:
           |  version: 0.2.0
           |  catalog:
           |    url: ${paths.cwd.resolve("runtime-catalog.yaml")}
           |repositories:
           |  car:
           |    - $repository
           |""".stripMargin)
      val launcher = new TextusLauncher(paths, FakeResolver(), FakeInvoker())

      val code = launcher.run(Vector(
        "install-cli",
        "sanpomap",
        "textus-sanpomap:0.2.0",
        "--operation-prefix",
        "sanpomap.presentation",
        "--bin-dir",
        bindir.toString,
        "--overwrite"
      ))

      _assert_equals(code, 0)
      Files.readString(bindir.resolve("sanpomap")).contains("artifact='textus-sanpomap:0.2.0'") shouldBe true
    }
  }

  def missingLocalAndRemoteCatalogArchivesReportBothDiagnostics(): Unit = _with_temp_paths { paths =>
    _write(paths.localRepository.resolve("repository").resolve("catalog").resolve("car").resolve("textus-missing.yaml"),
      _missing_catalog_text("0.2.0"))
    _with_http_repository(Map(
      "/repository/catalog/car/textus-missing.yaml" -> _missing_catalog_text("0.2.0")
    )) { repository =>
      val config = LauncherConfig(carRepositories = Vector(paths.localCarRepository.toString, repository))
      val failed =
        try {
          ArtifactResolver().resolve(ArtifactSelector("textus-missing", Some("0.2.0"), ArtifactKind.Car), config)
          false
        } catch {
          case e: TextusException =>
            e.getMessage.contains("local catalog points to a missing archive") &&
              e.getMessage.contains("remote catalog points to a missing archive")
        }

      failed shouldBe true
    }
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
    disabled shouldBe true
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
    Files.isRegularFile(paths.runtimeCatalog) shouldBe true
    launcher.run(Vector("runtime", "remote", "list"))
    launcher.run(Vector("runtime", "catalog", "show"))
    launcher.run(Vector("runtime", "channels"))
    launcher.run(Vector("runtime", "use", "recommended", "--project"))
    _assert_equals(Files.readString(paths.projectVersion).trim, "recommended")
    launcher.run(Vector("runtime", "current"))
  }

  def runtimeCurrentWarnsWhenCachedRecommendedIsStale(): Unit = _with_temp_paths { paths =>
    val remotecatalog = paths.cwd.resolve("runtime-catalog.yaml")
    _write(paths.runtimeCatalog, _catalog_text)
    _write(remotecatalog, _catalog_text.replace("recommended: 0.2.0", "recommended: 0.3.0-SNAPSHOT"))
    _write(paths.cwd.resolve(".textus").resolve("config.yaml"),
      s"""runtime:
         |  catalog:
         |    url: $remotecatalog
         |""".stripMargin)
    val launcher = new TextusLauncher(paths, CoursierCncfRuntimeResolver("false"), FakeInvoker())

    val (code, stdout, stderr) = _capture_stdout_stderr {
      launcher.run(Vector("runtime", "current"))
    }

    _assert_equals(code, 0)
    _assert_equals(stdout.trim, "0.2.0")
    stderr.contains("cached Textus runtime catalog resolves recommended to 0.2.0") shouldBe true
    stderr.contains("remote catalog resolves it to 0.3.0-SNAPSHOT") shouldBe true
    stderr.contains("textus runtime refresh") shouldBe true
  }

  def executionRewritesToCncfArgs(): Unit = _with_temp_paths { paths =>
    val carrepo = paths.cwd.resolve("repo").resolve("car")
    _write(carrepo.resolve("textus-blog").resolve("0.1.0").resolve("textus-blog-0.1.0.car"), "")
    _write(carrepo.resolve("textus-blog").resolve("0.1.1").resolve("textus-blog-0.1.1.car"), "")
    _write(carrepo.resolve("textus-blog").resolve("maven-metadata.xml"),
      """<?xml version="1.0" encoding="UTF-8"?>
        |<metadata>
        |  <groupId>org.simplemodeling.repository.car</groupId>
        |  <artifactId>textus-blog</artifactId>
        |  <versioning>
        |    <latest>0.1.1</latest>
        |    <release>0.1.1</release>
        |    <versions>
        |      <version>0.1.0</version>
        |      <version>0.1.1</version>
        |    </versions>
        |  </versioning>
        |</metadata>
        |""".stripMargin)
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
      s"--repository-dir=${paths.localCarRepository}",
      s"--repository-dir=${paths.cacheCarRepository}",
      "--repository-dir=https://www.simplemodeling.org/repository/car",
      s"--repository-dir=${paths.localSarRepository}",
      s"--repository-dir=${paths.cacheSarRepository}",
      "--repository-dir=https://www.simplemodeling.org/repository/sar",
      "--textus.component=textus-blog",
      "--textus.component.version=0.1.0",
      "command",
      "blog.post.search",
      "limit=10"
    ))
    launcher.run(Vector("command", "--car", "textus-blog", "blog.post.search", "limit=10"))
    _assert_equals(invoker.lastArgs, Vector(
      s"--repository-dir=$carrepo",
      s"--repository-dir=${paths.localCarRepository}",
      s"--repository-dir=${paths.cacheCarRepository}",
      "--repository-dir=https://www.simplemodeling.org/repository/car",
      s"--repository-dir=${paths.localSarRepository}",
      s"--repository-dir=${paths.cacheSarRepository}",
      "--repository-dir=https://www.simplemodeling.org/repository/sar",
      "--textus.component=textus-blog",
      "--textus.component.version=0.1.1",
      "command",
      "blog.post.search",
      "limit=10"
    ))
  }

  def textusAdminRegistrationLifecycle(): Unit = _with_temp_paths { paths =>
    Given("a Textus server launcher with opt-in Textus Admin registration")
    val carrepo = paths.cwd.resolve("repo").resolve("car")
    _write(carrepo.resolve("textus-registration").resolve("0.1.0").resolve("textus-registration-0.1.0.car"), "")
    _write(paths.cwd.resolve(".textus").resolve("config.yaml"),
      s"""repositories:
         |  car:
         |    - $carrepo
         |textus-admin:
         |  registration:
         |    enabled: true
         |    endpoint: https://admin.example.test/rest/v1/textus-admin/subsystem-inventory
         |    token-env: TEXTUS_ADMIN_REGISTRATION_TOKEN
         |    timeout: 2s
         |    heartbeat-interval: 30s
         |    host-label: acceptance
         |    base-url: https://subsystem.example.test
         |""".stripMargin)
    val reporter = FakeTextusAdminRegistrationReporter()
    val invoker = FakeInvoker()
    val launcher = new TextusLauncher(
      paths,
      FakeResolver(),
      invoker,
      environment = Map("TEXTUS_ADMIN_REGISTRATION_TOKEN" -> "secret-token"),
      registrationreporter = reporter
    )

    When("the canonical target-first server command completes")
    val code = launcher.run(Vector("textus-registration", "server"))

    Then("one Textus-owned registration lifecycle surrounds the runtime invocation without exposing the token")
    _assert_equals(code, 0)
    _assert_equals(reporter.starts.size, 1)
    _assert_equals(reporter.closes, 1)
    _assert_equals(reporter.starts.head._1.target, "textus-registration")
    _assert_equals(reporter.starts.head._1.subsystemVersion, Some("0.1.0"))
    _assert_equals(reporter.starts.head._2, Some("secret-token"))
    invoker.lastArgs.last shouldBe "server"

    And("a higher-priority explicit disable suppresses inherited registration")
    val inherited = LauncherConfig(
      textusAdminRegistration = Some(TextusAdminRegistrationConfig(
        endpoint = "https://admin.example.test/rest/v1/textus-admin/subsystem-inventory",
        tokenEnv = "TEXTUS_ADMIN_REGISTRATION_TOKEN",
        timeout = java.time.Duration.ofSeconds(2),
        heartbeatInterval = java.time.Duration.ofSeconds(30),
        hostLabel = "acceptance",
        baseUrl = "https://subsystem.example.test"
      )),
      textusAdminRegistrationEnabled = Some(true)
    )
    val disabled = LauncherConfig(textusAdminRegistrationEnabled = Some(false))
    inherited.mergeHigher(disabled).textusAdminRegistration shouldBe None

    And("registration setup failures do not prevent server startup")
    val unavailable = new ThrowingTextusAdminRegistrationReporter
    val outageinvoker = FakeInvoker()
    val outagelauncher = new TextusLauncher(
      paths,
      FakeResolver(),
      outageinvoker,
      environment = Map("TEXTUS_ADMIN_REGISTRATION_TOKEN" -> "secret-token"),
      registrationreporter = unavailable
    )
    val outagecode = outagelauncher.run(Vector("textus-registration", "server"))
    _assert_equals(outagecode, 0)
    outageinvoker.lastArgs.last shouldBe "server"
  }

  def textusAdminRegistrationHttpLifecycle(): Unit = {
    Given("a reachable Textus Admin automatic REST endpoint")
    val requests = scala.collection.mutable.ArrayBuffer.empty[(String, String)]
    val server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/", new HttpHandler {
      override def handle(exchange: HttpExchange): Unit = {
        requests += exchange.getRequestURI.toString -> exchange.getRequestHeaders.getFirst("Authorization")
        exchange.sendResponseHeaders(204, -1)
        exchange.close()
      }
    })
    server.start()
    try {
      val config = TextusAdminRegistrationConfig(
        endpoint = s"http://127.0.0.1:${server.getAddress.getPort}/rest/v1/textus-admin/subsystem-inventory",
        tokenEnv = "TEXTUS_ADMIN_REGISTRATION_TOKEN",
        timeout = java.time.Duration.ofSeconds(1),
        heartbeatInterval = java.time.Duration.ofSeconds(30),
        hostLabel = "acceptance",
        baseUrl = "https://subsystem.example.test"
      )
      val report = TextusAdminRegistrationReport(
        instanceId = "textus-registration-http-spec",
        target = "textus-registration",
        subsystemName = Some("textus-registration"),
        subsystemVersion = Some("0.1.0"),
        runtimeVersion = "0.5.0",
        startedAt = java.time.Instant.parse("2026-07-18T00:00:00Z")
      )

      When("the reporter starts and closes one server registration session")
      val session = TextusAdminRegistrationReporter.System.start(config, report, Some("test-token"))
      session.close()

      Then("it sends register and deregister operations with the configured bearer credential")
      requests.map(_._1).exists(_.contains("register-subsystem")) shouldBe true
      requests.map(_._1).exists(_.contains("deregister-subsystem")) shouldBe true
      requests.forall(_._2 == "Bearer test-token") shouldBe true
      requests.forall { case (path, _) => path.contains("instanceId=textus-registration-http-spec") } shouldBe true
    } finally {
      server.stop(0)
    }
  }

  def localRepositoryResolvesArtifactWithoutConfig(): Unit = _with_temp_paths { paths =>
    _write(paths.localCarRepository.resolve("textus-local").resolve("maven-metadata.xml"),
      """<metadata>
        |  <groupId>org.simplemodeling.repository.car</groupId>
        |  <artifactId>textus-local</artifactId>
        |  <versioning>
        |    <latest>0.1.0</latest>
        |    <versions><version>0.1.0</version></versions>
        |  </versioning>
        |</metadata>
        |""".stripMargin)
    _write(paths.localCarRepository.resolve("textus-local").resolve("0.1.0").resolve("textus-local-0.1.0.car"), "car")
    val invoker = FakeInvoker()
    val launcher = new TextusLauncher(paths, FakeResolver(), invoker)
    val code = launcher.run(Vector("server", "textus-local"))

    _assert_equals(code, 0)
    invoker.lastArgs.contains(s"--repository-dir=${paths.localCarRepository}") shouldBe true
    invoker.lastArgs.contains("--textus.component=textus-local") shouldBe true
    invoker.lastArgs.contains("--textus.component.version=0.1.0") shouldBe true
  }

  def snapshotArtifactDoesNotFallThroughToCacheOrPublic(): Unit = _with_temp_paths { paths =>
    _write(paths.cacheCarRepository.resolve("textus-snapshot").resolve("0.1.1-SNAPSHOT").resolve("textus-snapshot-0.1.1-SNAPSHOT.car"), "car")
    val invoker = FakeInvoker()
    val launcher = new TextusLauncher(paths, FakeResolver(), invoker)
    val failed =
      try {
        launcher.run(Vector("server", "textus-snapshot:0.1.1-SNAPSHOT"))
        false
      } catch {
        case e: TextusException =>
          e.getMessage.contains("snapshot component not found locally") &&
            e.getMessage.contains("cozyPublishLocalCar")
      }
    failed shouldBe true
    invoker.lastArgs.isEmpty shouldBe true
  }

  def artifactCatalogUsesCurrentCompatibleRuntimeByDefault(): Unit = _with_temp_paths { paths =>
    val carrepo = paths.cwd.resolve("repository").resolve("car")
    _write(carrepo.resolve("textus-blog").resolve("0.1.1").resolve("textus-blog-0.1.1.car"), "")
    _write(paths.cwd.resolve("repository").resolve("catalog").resolve("car").resolve("textus-blog.yaml"),
      _artifact_catalog_text("0.1.1", "0.2.0", Vector("0.2.0", "0.3.0-SNAPSHOT")))
    _write(paths.cwd.resolve("runtime-catalog.yaml"), _catalog_text)
    _write(paths.cwd.resolve(".textus").resolve("config.yaml"),
      s"""runtime:
         |  catalog:
         |    url: ${paths.cwd.resolve("runtime-catalog.yaml")}
         |repositories:
         |  car:
         |    - ${carrepo}
         |""".stripMargin)
    val resolver = FakeResolver()
    val launcher = new TextusLauncher(paths, resolver, FakeInvoker())

    launcher.run(Vector("command", "--car", "textus-blog", "blog.post.search"))

    _assert_equals(resolver.resolvedClasspaths, Vector("0.2.0"))
  }

  def artifactCatalogCanSelectLatestTestedRuntime(): Unit = _with_temp_paths { paths =>
    val carrepo = paths.cwd.resolve("repository").resolve("car")
    _write(carrepo.resolve("textus-blog").resolve("0.1.1").resolve("textus-blog-0.1.1.car"), "")
    _write(paths.cwd.resolve("repository").resolve("catalog").resolve("car").resolve("textus-blog.yaml"),
      _artifact_catalog_text("0.1.1", "0.2.0", Vector("0.2.0", "0.3.0-SNAPSHOT")))
    _write(paths.cwd.resolve("runtime-catalog.yaml"), _catalog_text)
    _write(paths.cwd.resolve(".textus").resolve("config.yaml"),
      s"""runtime:
         |  catalog:
         |    url: ${paths.cwd.resolve("runtime-catalog.yaml")}
         |repositories:
         |  car:
         |    - ${carrepo}
         |""".stripMargin)
    val resolver = FakeResolver()
    val launcher = new TextusLauncher(paths, resolver, FakeInvoker())

    launcher.run(Vector("--runtime-selection=tested-latest", "command", "--car", "textus-blog", "blog.post.search"))

    _assert_equals(resolver.resolvedClasspaths, Vector("0.2.0"))
  }

  def artifactCatalogCanSelectLatestCompatibleRuntime(): Unit = _with_temp_paths { paths =>
    val carrepo = paths.cwd.resolve("repository").resolve("car")
    _write(carrepo.resolve("textus-blog").resolve("0.1.1").resolve("textus-blog-0.1.1.car"), "")
    _write(paths.cwd.resolve("repository").resolve("catalog").resolve("car").resolve("textus-blog.yaml"),
      _artifact_catalog_text("0.1.1", "0.2.0", Vector("0.2.0")))
    _write(paths.cwd.resolve("runtime-catalog.yaml"), _catalog_text)
    _write(paths.cwd.resolve(".textus").resolve("config.yaml"),
      s"""runtime:
         |  catalog:
         |    url: ${paths.cwd.resolve("runtime-catalog.yaml")}
         |repositories:
         |  car:
         |    - ${carrepo}
         |""".stripMargin)
    val resolver = FakeResolver()
    val launcher = new TextusLauncher(paths, resolver, FakeInvoker())

    launcher.run(Vector("--runtime-selection=latest", "command", "--car", "textus-blog", "blog.post.search"))

    _assert_equals(resolver.resolvedClasspaths, Vector("0.2.0"))
  }

  def artifactCatalogCanSelectNewestCompatibleRuntime(): Unit = _with_temp_paths { paths =>
    val carrepo = paths.cwd.resolve("repository").resolve("car")
    _write(carrepo.resolve("textus-blog").resolve("0.1.1").resolve("textus-blog-0.1.1.car"), "")
    _write(paths.cwd.resolve("repository").resolve("catalog").resolve("car").resolve("textus-blog.yaml"),
      _artifact_catalog_text("0.1.1", "0.2.0", Vector("0.2.0")))
    _write(paths.cwd.resolve("runtime-catalog.yaml"), _catalog_text)
    _write(paths.cwd.resolve(".textus").resolve("config.yaml"),
      s"""runtime:
         |  catalog:
         |    url: ${paths.cwd.resolve("runtime-catalog.yaml")}
         |repositories:
         |  car:
         |    - ${carrepo}
         |""".stripMargin)
    val resolver = FakeResolver()
    val launcher = new TextusLauncher(paths, resolver, FakeInvoker())

    launcher.run(Vector("--runtime-selection=newest", "command", "--car", "textus-blog", "blog.post.search"))

    _assert_equals(resolver.resolvedClasspaths, Vector("0.3.0-SNAPSHOT"))
  }

  def artifactCatalogIncludesDependencyRequirements(): Unit = _with_temp_paths { paths =>
    val carrepo = paths.cwd.resolve("repository").resolve("car")
    _write(carrepo.resolve("textus-blog").resolve("0.1.1").resolve("textus-blog-0.1.1.car"), "")
    _write(carrepo.resolve("textus-user-account").resolve("0.2.0").resolve("textus-user-account-0.2.0.car"), "")
    _write(paths.cwd.resolve("repository").resolve("catalog").resolve("car").resolve("textus-blog.yaml"),
      _artifact_catalog_text("0.1.1", "0.2.0", Vector("0.2.0", "0.3.0-SNAPSHOT")).replace(
        "aliases: []",
        "aliases: []\ndependencies:\n  car:\n    - textus-user-account:0.2.0"
      ))
    _write(paths.cwd.resolve("repository").resolve("catalog").resolve("car").resolve("textus-user-account.yaml"),
      _artifact_catalog_text("0.2.0", "0.2.0", Vector("0.2.0")))
    _write(paths.cwd.resolve("runtime-catalog.yaml"), _catalog_text)
    _write(paths.cwd.resolve(".textus").resolve("config.yaml"),
      s"""runtime:
         |  catalog:
         |    url: ${paths.cwd.resolve("runtime-catalog.yaml")}
         |repositories:
         |  car:
         |    - ${carrepo}
         |""".stripMargin)
    val resolver = FakeResolver()
    val launcher = new TextusLauncher(paths, resolver, FakeInvoker())

    launcher.run(Vector("command", "--car", "textus-blog", "blog.post.search"))

    _assert_equals(resolver.resolvedClasspaths, Vector("0.2.0"))
  }

  def artifactCatalogDoesNotFallbackToMetadataWhenCatalogRejectsVersion(): Unit = _with_temp_paths { paths =>
    val carrepo = paths.cwd.resolve("repository").resolve("car")
    _write(carrepo.resolve("textus-blog").resolve("0.1.1").resolve("textus-blog-0.1.1.car"), "")
    _write(carrepo.resolve("textus-blog").resolve("maven-metadata.xml"),
      """<?xml version="1.0" encoding="UTF-8"?>
        |<metadata>
        |  <artifactId>textus-blog</artifactId>
        |  <versioning>
        |    <latest>0.1.1</latest>
        |    <versions><version>0.1.1</version></versions>
        |  </versioning>
        |</metadata>
        |""".stripMargin)
    _write(paths.cwd.resolve("repository").resolve("catalog").resolve("car").resolve("textus-blog.yaml"),
      _artifact_catalog_text("0.1.1", "0.2.0", Vector("0.2.0")).replace("status: active", "status: disabled"))
    _write(paths.cwd.resolve("runtime-catalog.yaml"), _catalog_text)
    _write(paths.cwd.resolve(".textus").resolve("config.yaml"),
      s"""runtime:
         |  catalog:
         |    url: ${paths.cwd.resolve("runtime-catalog.yaml")}
         |repositories:
         |  car:
         |    - ${carrepo}
         |""".stripMargin)
    val launcher = new TextusLauncher(paths, FakeResolver(), FakeInvoker())
    val failed =
      try {
        launcher.run(Vector("command", "--car", "textus-blog", "blog.post.search"))
        false
      } catch {
        case e: TextusException => e.getMessage.contains("artifact catalog does not contain an enabled version")
      }
    failed shouldBe true
  }


  def runtimeConflictDefaultsToError(): Unit = _with_temp_paths { paths =>
    val carrepo = paths.cwd.resolve("repository").resolve("car")
    _write(carrepo.resolve("textus-blog").resolve("0.1.1").resolve("textus-blog-0.1.1.car"), "")
    _write(paths.cwd.resolve("repository").resolve("catalog").resolve("car").resolve("textus-blog.yaml"),
      _artifact_catalog_text("0.1.1", "9.0.0", Vector("9.0.0")))
    _write(paths.cwd.resolve("runtime-catalog.yaml"), _catalog_text)
    _write(paths.cwd.resolve(".textus").resolve("config.yaml"),
      s"""runtime:
         |  catalog:
         |    url: ${paths.cwd.resolve("runtime-catalog.yaml")}
         |repositories:
         |  car:
         |    - ${carrepo}
         |""".stripMargin)
    val launcher = new TextusLauncher(paths, FakeResolver(), FakeInvoker())
    val failed =
      try {
        launcher.run(Vector("command", "--car", "textus-blog", "blog.post.search"))
        false
      } catch {
        case e: TextusException => e.getMessage.contains("no compatible CNCF runtime version")
      }
    failed shouldBe true
  }

  def runtimeConflictCanUseNewestPolicy(): Unit = _with_temp_paths { paths =>
    val carrepo = paths.cwd.resolve("repository").resolve("car")
    _write(carrepo.resolve("textus-blog").resolve("0.1.1").resolve("textus-blog-0.1.1.car"), "")
    _write(paths.cwd.resolve("repository").resolve("catalog").resolve("car").resolve("textus-blog.yaml"),
      _artifact_catalog_text("0.1.1", "9.0.0", Vector("9.0.0")))
    _write(paths.cwd.resolve("runtime-catalog.yaml"), _catalog_text)
    _write(paths.cwd.resolve(".textus").resolve("config.yaml"),
      s"""runtime:
         |  catalog:
         |    url: ${paths.cwd.resolve("runtime-catalog.yaml")}
         |repositories:
         |  car:
         |    - ${carrepo}
         |""".stripMargin)
    val resolver = FakeResolver()
    val launcher = new TextusLauncher(paths, resolver, FakeInvoker())

    launcher.run(Vector("--runtime-no-compatible=newest", "command", "--car", "textus-blog", "blog.post.search"))

    _assert_equals(resolver.resolvedClasspaths, Vector("0.3.0-SNAPSHOT"))
  }

  def explicitRuntimeIsValidatedAgainstArtifactRequirement(): Unit = _with_temp_paths { paths =>
    val carrepo = paths.cwd.resolve("repository").resolve("car")
    _write(carrepo.resolve("textus-blog").resolve("0.1.1").resolve("textus-blog-0.1.1.car"), "")
    _write(paths.cwd.resolve("repository").resolve("catalog").resolve("car").resolve("textus-blog.yaml"),
      _artifact_catalog_text("0.1.1", "0.2.0", Vector("0.2.0")))
    _write(paths.cwd.resolve("runtime-catalog.yaml"), _catalog_text)
    _write(paths.cwd.resolve(".textus").resolve("config.yaml"),
      s"""runtime:
         |  catalog:
         |    url: ${paths.cwd.resolve("runtime-catalog.yaml")}
         |repositories:
         |  car:
         |    - ${carrepo}
         |""".stripMargin)
    val launcher = new TextusLauncher(paths, FakeResolver(), FakeInvoker())
    val failed =
      try {
        launcher.run(Vector("--runtime", "0.1.0", "command", "--car", "textus-blog", "blog.post.search"))
        false
      } catch {
        case e: TextusException => e.getMessage.contains("not compatible") || e.getMessage.contains("disabled")
      }
    failed shouldBe true
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
    _write(paths.cwd.resolve(".textus").resolve("config.yaml"),
      s"""runtime:
         |  catalog:
         |    url: ${paths.cwd.resolve("missing-runtime-catalog.yaml")}
         |""".stripMargin)
    val resolver = FakeResolver()
    val launcher = new TextusLauncher(paths, resolver, FakeInvoker())
    launcher.run(Vector("runtime", "current"))
    _assert_equals(resolver.resolvedVersions, Vector(LauncherConfig.DEFAULT_RUNTIME_VERSION))
  }

  def noRuntimeLibraryDependencies(): Unit = {
    val lines = Files.readString(Path.of("build.sbt")).linesIterator.toVector.map(_.trim)
    def _runtime_library_dependency_(line: String): Boolean =
      line.contains("libraryDependencies") &&
        line.contains("\"") &&
        !line.contains("goldenport-launcher-core") &&
        !line.contains("% Test") &&
        !line.contains("% \"test\"")
    lines.exists(_runtime_library_dependency_) shouldBe false
    lines.exists(_.contains("goldenport-launcher-core")) shouldBe true
  }

  private def _capture_stdout(f: => Int): (Int, String) = {
    val out = new java.io.ByteArrayOutputStream()
    val code = Console.withOut(new java.io.PrintStream(out)) {
      f
    }
    (code, out.toString)
  }

  private def _capture_stdout_stderr(f: => Int): (Int, String, String) = {
    val out = new java.io.ByteArrayOutputStream()
    val err = new java.io.ByteArrayOutputStream()
    val code = Console.withOut(new java.io.PrintStream(out)) {
      Console.withErr(new java.io.PrintStream(err)) {
        f
      }
    }
    (code, out.toString, err.toString)
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

  private def _with_http_repository(files: Map[String, String])(f: String => Unit): Unit = {
    val server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/", new HttpHandler {
      override def handle(exchange: HttpExchange): Unit = {
        files.get(exchange.getRequestURI.getPath) match {
          case Some(content) =>
            val bytes = content.getBytes(StandardCharsets.UTF_8)
            exchange.sendResponseHeaders(200, if (exchange.getRequestMethod == "HEAD") -1 else bytes.length)
            if (exchange.getRequestMethod != "HEAD")
              exchange.getResponseBody.write(bytes)
          case None =>
            exchange.sendResponseHeaders(404, -1)
        }
        exchange.close()
      }
    })
    server.start()
    try f(s"http://127.0.0.1:${server.getAddress.getPort}/repository/car")
    finally server.stop(0)
  }

  private def _assert_equals[A](actual: A, expected: A): Unit =
    actual shouldBe expected

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

  private def _artifact_catalog_text(
    version: String,
    minimum: String,
    tested: Vector[String]
  ): String =
    s"""schemaVersion: 1
       |kind: car
       |artifactId: textus-blog
       |recommended: $version
       |latestStable: $version
       |aliases: []
       |versions:
       |  - version: $version
       |    channel: stable
       |    status: active
       |    file: repository/car/textus-blog/$version/textus-blog-$version.car
       |    runtime:
       |      cncf:
       |        minimum: $minimum
       |        excluded: []
       |        tested:
       |${tested.map(v => s"          - $v").mkString("\n")}
       |""".stripMargin

  private def _sanpomap_catalog_text(version: String): String =
    s"""schemaVersion: 1
       |kind: car
       |artifactId: textus-sanpomap
       |recommended: $version
       |latestStable: $version
       |versions:
       |  - version: $version
       |    channel: stable
       |    status: active
       |    file: repository/car/textus-sanpomap/$version/textus-sanpomap-$version.car
       |""".stripMargin

  private def _missing_catalog_text(version: String): String =
    s"""schemaVersion: 1
       |kind: car
       |artifactId: textus-missing
       |recommended: $version
       |latestStable: $version
       |versions:
       |  - version: $version
       |    channel: stable
       |    status: active
       |    file: repository/car/textus-missing/$version/textus-missing-$version.car
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

final class FakeTextusLauncherDevInvoker extends TextusLauncherDevInvoker {
  var devDir: Option[Path] = None
  var args: Vector[String] = Vector.empty
  var cwd: Option[Path] = None

  override def invoke(value: Path, commandargs: Vector[String], valuecwd: Path): Int = {
    devDir = Some(value)
    args = commandargs
    cwd = Some(valuecwd)
    0
  }
}

object FakeTextusLauncherDevInvoker {
  def apply(): FakeTextusLauncherDevInvoker = new FakeTextusLauncherDevInvoker()
}

final class FakeClasspathExporter(classpath: String) extends RuntimeClasspathExporter {
  var projects: Vector[Path] = Vector.empty

  def exportRuntimeClasspath(project: Path): String = {
    projects :+= project
    classpath
  }
}

object FakeClasspathExporter {
  def apply(classpath: String): FakeClasspathExporter = new FakeClasspathExporter(classpath)
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

final class FakeTextusAdminRegistrationReporter extends TextusAdminRegistrationReporter {
  var starts: Vector[(TextusAdminRegistrationReport, Option[String])] = Vector.empty
  var closes: Int = 0

  def start(
    config: TextusAdminRegistrationConfig,
    report: TextusAdminRegistrationReport,
    token: Option[String]
  ): TextusAdminRegistrationSession = {
    starts :+= report -> token
    new TextusAdminRegistrationSession {
      def close(): Unit = closes += 1
    }
  }
}

object FakeTextusAdminRegistrationReporter {
  def apply(): FakeTextusAdminRegistrationReporter = new FakeTextusAdminRegistrationReporter()
}

final class ThrowingTextusAdminRegistrationReporter extends TextusAdminRegistrationReporter {
  def start(
    config: TextusAdminRegistrationConfig,
    report: TextusAdminRegistrationReport,
    token: Option[String]
  ): TextusAdminRegistrationSession =
    throw TextusException("simulated Textus Admin outage")
}

object FakeInvoker {
  def apply(): FakeInvoker = new FakeInvoker()
}

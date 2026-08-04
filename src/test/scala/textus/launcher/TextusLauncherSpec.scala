package textus.launcher

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import java.net.InetSocketAddress
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.concurrent.ConcurrentLinkedQueue
import scala.jdk.CollectionConverters.*

/*
 * @since   May. 17, 2026
 *  version May. 27, 2026
 *  version Jun. 29, 2026
 *  version Jul. 27, 2026
 * @version Aug. 5, 2026
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
    spec.serverExecutionDelegatesDefaultPortResolutionToRuntime()
    spec.textusControlCenterRegistrationLifecycle()
    spec.localServerEvidenceRetentionAndRecovery()
    spec.standaloneControlCenterLocatorLifecycle()
    spec.textusControlCenterRegistrationHttpLifecycle()
    spec.lifecycleSupervisorDelegateLifecycle()
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
    spec.componentRepositoryCommandParser()
    spec.componentRepositoryLocalPrecedenceAndConflictDiagnostics()
    spec.componentRepositoryRefreshIsBoundedAndPreservesStaleCache()
    spec.componentRepositoryShowValidatesDetailedCatalog()
    spec.componentRepositoryShowValidatesJsonDetailedCatalog()
    spec.componentRepositoryShowPreservesValidatedDetailCache()
    spec.latestRuntimeIsConcrete()
    spec.noCncfRuntimeLibraryDependencies()
    println("TextusLauncherSpec: OK")
  }
}

final class TextusLauncherSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "textus launcher" should {
    "command parsing" which {
      "parser" in {
        Given("the textus launcher scenario: parser")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(parser())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

      "runtime catalog parse and selector resolution" in {
        Given("the textus launcher scenario: runtime catalog parse and selector resolution")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(runtimeCatalogParseAndSelectorResolution())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

    }

    "configuration and launcher metadata" which {
      "launcher version" in {
        Given("the textus launcher scenario: launcher version")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(launcherVersion())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

      "config merge" in {
        Given("the textus launcher scenario: config merge")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(configMerge())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

      "workspace root config applies to nested cwd" in {
        Given("the textus launcher scenario: workspace root config applies to nested cwd")
        When("the launcher loads config from a nested sample directory")
        val outcome = scala.util.Try(workspaceRootConfigAppliesToNestedCwd())
        Then("the executable specification holds through inherited root config")
        outcome.get shouldBe ()
      }

      "launcher config supports properties and conf files" in {
        Given("the textus launcher scenario: launcher config supports properties and conf files")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(launcherConfigSupportsPropertiesAndConfFiles())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

      "launcher dev dir delegates to development launcher" in {
        Given("a global Textus launcher config selects a development launcher checkout")
        When("the launcher command is run")
        val outcome = scala.util.Try(launcherDevDirDelegatesToDevelopmentLauncher())
        Then("the configured checkout receives the original command arguments")
        outcome.get shouldBe ()
      }

      "launcher dev dir rejects stale development classpath" in {
        Given("a Textus development launcher classpath without the Textus main class")
        When("the launcher tries to delegate")
        val outcome = scala.util.Try(launcherDevDirRejectsStaleDevelopmentClasspath())
        Then("the stale classpath is rejected before starting a process")
        outcome.get shouldBe ()
      }

      "runtime development config" in {
        Given("a Textus launcher config declares a development CNCF runtime checkout")
        When("development mode or environment override is supplied")
        val outcome = scala.util.Try(runtimeDevelopmentConfig())
        Then("the runtime development directory is activated through launcher config")
        outcome.get shouldBe ()
      }

      "local repository resolves artifact without config" in {
        Given("the textus launcher scenario: local repository resolves artifact without config")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(localRepositoryResolvesArtifactWithoutConfig())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

    }

    "runtime selection and catalog operations" which {
      "runtime version" in {
        Given("the textus launcher scenario: runtime version")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(runtimeVersion())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

      "runtime help" in {
        Given("the textus launcher scenario: runtime help")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(runtimeHelp())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

      "runtime version precedence" in {
        Given("the textus launcher scenario: runtime version precedence")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(runtimeVersionPrecedence())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

      "runtime use writes expected files" in {
        Given("the textus launcher scenario: runtime use writes expected files")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(runtimeUseWritesExpectedFiles())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

      "runtime use auto selects project when textus directory exists" in {
        Given("the textus launcher scenario: runtime use auto selects project when textus directory exists")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(runtimeUseAutoSelectsProjectWhenTextusDirectoryExists())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

      "install cli writes user facing command" in {
        Given("the textus launcher scenario: install cli writes user facing command")
        When("the launcher installs a domain command")
        val outcome = scala.util.Try(installCliWritesUserFacingCommand())
        Then("the command delegates to textus command with file parameter expansion")
        outcome.get shouldBe ()
      }

      "install cli falls back from stale local catalog to remote CAR" in {
        Given("a stale local CAR catalog whose archive is missing")
        When("the launcher installs a command for a published CAR available remotely")
        val outcome = scala.util.Try(installCliFallsBackFromStaleLocalCatalogToRemoteCar())
        Then("the command wrapper is generated from the remote CAR version")
        outcome.get shouldBe ()
      }

      "missing local and remote catalog archives report both diagnostics" in {
        Given("local and remote catalogs whose selected archives are both missing")
        When("the resolver cannot locate the requested CAR")
        val outcome = scala.util.Try(missingLocalAndRemoteCatalogArchivesReportBothDiagnostics())
        Then("the error identifies both failed catalog sources")
        outcome.get shouldBe ()
      }

      "runtime catalog commands" in {
        Given("the textus launcher scenario: runtime catalog commands")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(runtimeCatalogCommands())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

      "runtime current warns when cached recommended is stale" in {
        Given("the textus launcher scenario: runtime current warns when cached recommended is stale")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(runtimeCurrentWarnsWhenCachedRecommendedIsStale())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

      "runtime conflict defaults to error" in {
        Given("the textus launcher scenario: runtime conflict defaults to error")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(runtimeConflictDefaultsToError())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

      "runtime conflict can use newest policy" in {
        Given("the textus launcher scenario: runtime conflict can use newest policy")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(runtimeConflictCanUseNewestPolicy())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

      "runtime command does not load cncf" in {
        Given("the textus launcher scenario: runtime command does not load cncf")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(runtimeCommandDoesNotLoadCncf())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

      "latest runtime is concrete" in {
        Given("the textus launcher scenario: latest runtime is concrete")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(latestRuntimeIsConcrete())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

    }

    "artifact execution and resolution" which {
      "execution rewrites to cncf args" in {
        Given("the textus launcher scenario: execution rewrites to cncf args")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(executionRewritesToCncfArgs())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

      "server execution delegates default port resolution to the runtime" in {
        serverExecutionDelegatesDefaultPortResolutionToRuntime()
      }

      "canonical server commands report one Textus Control Center lifecycle" in {
        textusControlCenterRegistrationLifecycle()
      }

      "retain bounded shared evidence and preserve malformed evidence for recovery" in {
        localServerEvidenceRetentionAndRecovery()
      }

      "canonical server commands resolve one standalone Control Center locator" in {
        standaloneControlCenterLocatorLifecycle()
      }

      "Textus Control Center reporter calls automatic REST operations" in {
        textusControlCenterRegistrationHttpLifecycle()
      }

      "snapshot artifact does not fall through to cache or public" in {
        Given("the textus launcher scenario: snapshot artifact does not fall through to cache or public")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(snapshotArtifactDoesNotFallThroughToCacheOrPublic())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

      "artifact catalog uses current compatible runtime by default" in {
        Given("the textus launcher scenario: artifact catalog uses current compatible runtime by default")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(artifactCatalogUsesCurrentCompatibleRuntimeByDefault())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

      "artifact catalog can select latest tested runtime" in {
        Given("the textus launcher scenario: artifact catalog can select latest tested runtime")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(artifactCatalogCanSelectLatestTestedRuntime())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

      "artifact catalog can select latest compatible runtime" in {
        Given("the textus launcher scenario: artifact catalog can select latest compatible runtime")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(artifactCatalogCanSelectLatestCompatibleRuntime())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

      "artifact catalog can select newest compatible runtime" in {
        Given("the textus launcher scenario: artifact catalog can select newest compatible runtime")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(artifactCatalogCanSelectNewestCompatibleRuntime())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

      "artifact catalog includes dependency requirements" in {
        Given("the textus launcher scenario: artifact catalog includes dependency requirements")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(artifactCatalogIncludesDependencyRequirements())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

      "artifact catalog does not fallback to metadata when catalog rejects version" in {
        Given("the textus launcher scenario: artifact catalog does not fallback to metadata when catalog rejects version")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(artifactCatalogDoesNotFallbackToMetadataWhenCatalogRejectsVersion())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

      "explicit runtime is validated against artifact requirement" in {
        Given("the textus launcher scenario: explicit runtime is validated against artifact requirement")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(explicitRuntimeIsValidatedAgainstArtifactRequirement())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

    }

    "packaging boundaries" which {
      "no CNCF runtime library dependencies" in {
        Given("the textus launcher scenario: no runtime library dependencies")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(noCncfRuntimeLibraryDependencies())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

    }

    "launcher behavior" which {
      "help explains local repository" in {
        Given("the textus launcher scenario: help explains local repository")
        When("the launcher behavior is exercised")
        val outcome = scala.util.Try(helpExplainsLocalRepository())
        Then("the executable specification holds through scenario-specific expectations")
        outcome.get shouldBe ()
      }

    }

    "component repository discovery" which {
      "parse list, show, and refresh commands" in {
        Given("component repository discovery command arguments")
        When("the Textus command parser reads list, show, and refresh forms")
        val outcome = scala.util.Try(componentRepositoryCommandParser())
        Then("artifact kind and configured source selectors remain explicit")
        outcome.get shouldBe ()
      }

      "prefer local entries and diagnose equal-precedence conflicts" in {
        Given("local and cached indexes that expose overlapping component identities")
        When("Textus lists component repository artifacts without network access")
        val outcome = scala.util.Try(componentRepositoryLocalPrecedenceAndConflictDiagnostics())
        Then("local entries win and equal-precedence local conflicts are deterministic")
        outcome.get shouldBe ()
      }

      "bound refresh and preserve stale cache" in {
        Given("an explicitly configured remote repository and a previously valid cached index")
        When("refresh later receives a malformed index")
        val outcome = scala.util.Try(componentRepositoryRefreshIsBoundedAndPreservesStaleCache())
        Then("only the fixed index URL is requested and the stale valid cache remains listable")
        outcome.get shouldBe ()
      }

      "validate detailed catalogs on show" in {
        Given("a local repository index and its detailed artifact catalog")
        When("Textus shows the selected component")
        val outcome = scala.util.Try(componentRepositoryShowValidatesDetailedCatalog())
        Then("the index identity and selectors are checked against the detailed catalog")
        outcome.get shouldBe ()
      }

      "validate JSON detailed catalogs on show" in {
        Given("a local repository index that references a JSON artifact catalog")
        When("Textus shows the selected component")
        val outcome = scala.util.Try(componentRepositoryShowValidatesJsonDetailedCatalog())
        Then("the JSON identity and selectors satisfy the same detail contract as YAML")
        outcome.get shouldBe ()
      }

      "preserve validated detail cache" in {
        Given("a validated remote detailed catalog and a later mismatched response")
        When("Textus shows the same component again")
        val outcome = scala.util.Try(componentRepositoryShowPreservesValidatedDetailCache())
        Then("the mismatched response is not cached and stale validated detail use is diagnosed")
        outcome.get shouldBe ()
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
    help.contains("--runtime-dev-dir") shouldBe false
    help.contains("Config launcher.dev-dir delegates the installed launcher") shouldBe true
    help.contains("sbt textusExportLauncherClasspath") shouldBe true
    help.contains("runtime.dev-dir") shouldBe false
    help.contains("development.runtime.dev-dir") shouldBe false
    help.contains("TEXTUS_USE_DEVELOPMENT=true") shouldBe false
    help.contains("TEXTUS_RUNTIME_DEV_DIR") shouldBe false
    help.contains("--launcher-home <dir> selects an explicit Launcher state-home root") shouldBe true
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
    Files.createDirectories(classdir)
    _write(devdir.resolve("target").resolve("cncf.d").resolve("runtime-classpath.txt"), classdir.toString)
    val devinvoker = FakeInvoker()
    val devlauncher = new TextusLauncher(paths, FakeResolver(), devinvoker)

    When("the launcher version command is executed against the runtime development directory")
    val devcode = devlauncher.run(Vector("--runtime-dev-dir", devdir.toString, "version"))

    Then("the launcher invokes the development CNCF runtime version command")
    _assert_equals(devcode, 0)
    _assert_equals(devinvoker.lastClasspath, Vector(classdir))
    _assert_equals(devinvoker.lastArgs, Vector("version"))
    _assert_equals(
      TextusCommandParser.parse(Vector("--runtime-dev-dir", devdir.toString, "version")),
      TextusCommand.Runtime.Version(None, Some(devdir.toString))
    )

    Given("a runtime development directory without a prepared classpath")
    val missingdir = paths.cwd.resolve("cncf-missing")
    val missinglauncher = new TextusLauncher(paths, FakeResolver(), FakeInvoker())

    When("the launcher version command is executed against the unprepared directory")
    val failure = intercept[TextusException] {
      missinglauncher.run(Vector("--runtime-dev-dir", missingdir.toString, "version"))
    }

    Then("the launcher rejects the invocation without using SBT")
    failure.getMessage.contains("development runtime classpath not found") shouldBe true
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
    val launcher = new TextusLauncher(paths, FakeResolver(), FakeInvoker(), invoker)

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
    val isolatedhome = paths.cwd.resolve("isolated-launcher-home")
    launcher.run(Vector("--launcher-home", isolatedhome.toString, "runtime", "use", "0.4.0", "--global"))
    _assert_equals(Files.readString(isolatedhome.resolve(".textus").resolve("version")).trim, "0.4.0")
    _assert_equals(Files.readString(paths.globalVersion).trim, "0.2.0")
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

  def serverExecutionDelegatesDefaultPortResolutionToRuntime(): Unit = _with_temp_paths { paths =>
    Given("resolved CAR and SAR server invocations without an explicit server port")
    val carrepo = paths.cwd.resolve("repo").resolve("car")
    val sarrepo = paths.cwd.resolve("repo").resolve("sar")
    _write(carrepo.resolve("textus-blog").resolve("0.1.0").resolve("textus-blog-0.1.0.car"), "")
    _write(sarrepo.resolve("textus-platform").resolve("0.1.0").resolve("textus-platform-0.1.0.sar"), "")
    _write(paths.cwd.resolve(".textus").resolve("config.yaml"),
      s"""runtime:
         |  version: 0.5.0
         |  catalog:
         |    url: ${paths.cwd.resolve("missing-runtime-catalog.yaml")}
         |repositories:
         |  car:
         |    - $carrepo
         |  sar:
         |    - $sarrepo
         |""".stripMargin)
    val invoker = FakeInvoker()
    val launcher = new TextusLauncher(paths, FakeResolver(), invoker)

    When("the launcher delegates the CAR invocation to CNCF")
    val carcode = launcher.run(Vector("textus-blog:0.1.0", "server"))

    Then("CAR activation is forwarded without a launcher-owned port override")
    _assert_equals(carcode, 0)
    invoker.lastArgs.contains("--textus.component=textus-blog") shouldBe true
    invoker.lastArgs.contains("--textus.component.version=0.1.0") shouldBe true
    invoker.lastArgs.last shouldBe "server"
    invoker.lastArgs.exists(_.startsWith("--textus.server.port=")) shouldBe false
    invoker.lastArgs.exists(_.startsWith("--cncf.server.port=")) shouldBe false

    When("a runtime passthrough argument happens to use the Launcher state-home option name")
    val passthroughhome = "runtime-passthrough-home"
    val passthroughcode = launcher.run(Vector("textus-blog:0.1.0", "server", "--", "--launcher-home", passthroughhome))

    Then("the Launcher preserves it for the component runtime without changing its own state home")
    _assert_equals(passthroughcode, 0)
    invoker.lastArgs.contains("--launcher-home") shouldBe true
    invoker.lastArgs.contains(passthroughhome) shouldBe true
    Files.exists(paths.cwd.resolve(passthroughhome).resolve(".textus")) shouldBe false

    When("the launcher delegates the SAR invocation to CNCF")
    val sarcode = launcher.run(Vector("textus-platform.sar:0.1.0", "server"))

    Then("SAR activation is forwarded without a launcher-owned port override")
    _assert_equals(sarcode, 0)
    invoker.lastArgs.contains("--textus.subsystem=textus-platform-0.1.0") shouldBe true
    invoker.lastArgs.last shouldBe "server"
    invoker.lastArgs.exists(_.startsWith("--textus.server.port=")) shouldBe false
    invoker.lastArgs.exists(_.startsWith("--cncf.server.port=")) shouldBe false
  }

  def textusControlCenterRegistrationLifecycle(): Unit = _with_temp_paths { paths =>
    Given("a Textus server launcher with opt-in Textus Control Center registration")
    val carrepo = paths.cwd.resolve("repo").resolve("car")
    _write(carrepo.resolve("textus-registration").resolve("0.1.0").resolve("textus-registration-0.1.0.car"), "")
    _write(paths.cwd.resolve(".textus").resolve("config.yaml"),
      s"""repositories:
         |  car:
         |    - $carrepo
         |textus-control-center:
         |  registration:
         |    enabled: true
         |    endpoint: https://admin.example.test/rest/v1/textus-control-center/subsystem-inventory
         |    token-env: TEXTUS_ADMIN_REGISTRATION_TOKEN
         |    timeout: 2s
         |    heartbeat-interval: 30s
         |    host-label: acceptance
         |    base-url: https://subsystem.example.test
         |""".stripMargin)
    val reporter = FakeTextusControlCenterRegistrationReporter()
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
    _assert_equals(reporter.starts.head._1.artifactId, Some("textus-registration"))
    _assert_equals(reporter.starts.head._1.executionMode, "artifact")
    _assert_equals(reporter.starts.head._1.developmentDirectory, None)
    _assert_equals(reporter.starts.head._1.subsystemVersion, Some("0.1.0"))
    _assert_equals(reporter.starts.head._2, Some("secret-token"))
    invoker.lastArgs.last shouldBe "server"
    _entries(paths).map(_.launcherKind) should contain ("textus")
    _entries(paths).map(_.stoppedAt.isDefined) should contain (true)

    And("a higher-priority explicit disable suppresses inherited registration")
    val inherited = LauncherConfig(
      textusControlCenterRegistration = Some(TextusControlCenterRegistrationConfig(
        endpoint = "https://admin.example.test/rest/v1/textus-control-center/subsystem-inventory",
        tokenEnv = "TEXTUS_ADMIN_REGISTRATION_TOKEN",
        timeout = java.time.Duration.ofSeconds(2),
        heartbeatInterval = java.time.Duration.ofSeconds(30),
        hostLabel = "acceptance",
        baseUrl = "https://subsystem.example.test"
      )),
      textusControlCenterRegistrationEnabled = Some(true)
    )
    val disabled = LauncherConfig(textusControlCenterRegistrationEnabled = Some(false))
    inherited.mergeHigher(disabled).textusControlCenterRegistration shouldBe None

    And("registration setup failures do not prevent server startup")
    val unavailable = new ThrowingTextusControlCenterRegistrationReporter
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

  def localServerEvidenceRetentionAndRecovery(): Unit = _with_temp_paths { paths =>
    import LocalServerEvidenceSnapshot.given
    import io.circe.syntax.*

    Given("stale, bounded, malformed, and independently written shared evidence")
    val now = java.time.Instant.now()
    val stale = LocalServerEvidenceEntry("textus", "stale", "stale", Some("stale"), "artifact", None, None, None, "0.5.0", now.minus(LocalServerEvidenceStore.Retention).minusSeconds(1), now.minus(LocalServerEvidenceStore.Retention).minusSeconds(1), Some(now.minus(LocalServerEvidenceStore.Retention).minusSeconds(1)))
    _write(paths.serverEvidence, LocalServerEvidenceSnapshot(LocalServerEvidenceSnapshot.Schema, Vector(stale)).asJson.noSpaces)
    val store = LocalServerEvidenceStore(paths)

    When("a canonical Launcher update follows the retention boundary")
    store.started(_local_evidence_report("fresh"), "textus")
    val retained = _entries(paths).map(_.instanceId)

    And("a bounded store receives one entry beyond its maximum")
    val bounded = Vector.tabulate(LocalServerEvidenceStore.MaximumEntries)(index =>
      LocalServerEvidenceEntry("textus", s"bounded-$index", s"bounded-$index", Some(s"bounded-$index"), "artifact", None, None, None, "0.5.0", now, now, None)
    )
    _write(paths.serverEvidence, LocalServerEvidenceSnapshot(LocalServerEvidenceSnapshot.Schema, bounded).asJson.noSpaces)
    store.started(_local_evidence_report("bounded-fresh"), "textus")
    val capped = _entries(paths).map(_.instanceId)

    And("the next update encounters malformed content before an independent writer update")
    _write(paths.serverEvidence, "{malformed")
    store.started(_local_evidence_report("recovered"), "textus")
    LocalServerEvidenceStore(paths).started(_local_evidence_report("cncf-writer"), "cncf")
    val recovered = _entries(paths).map(_.instanceId)
    val files = Files.list(paths.serverEvidence.getParent)
    val recovery = try files.iterator.asScala.toVector.find(_.getFileName.toString.startsWith("server-evidence.recovery-")) finally files.close()

    Then("stale entries are pruned, mutation order enforces the cap, malformed bytes are retained, and writers preserve each other")
    retained shouldBe Vector("fresh")
    capped should have size LocalServerEvidenceStore.MaximumEntries
    capped should not contain "bounded-0"
    capped should contain("bounded-fresh")
    recovered should contain allOf ("recovered", "cncf-writer")
    recovery.map(Files.readString) shouldBe Some("{malformed")
  }

  def standaloneControlCenterLocatorLifecycle(): Unit = _with_temp_paths { paths =>
    Given("a machine-local standalone Control Center locator and owner-only launcher token")
    val carrepo = paths.cwd.resolve("repo").resolve("car")
    _write(carrepo.resolve("textus-registration").resolve("0.1.0").resolve("textus-registration-0.1.0.car"), "")
    _write(paths.cwd.resolve(".textus").resolve("config.yaml"),
      s"""repositories:
         |  car:
         |    - $carrepo
         |""".stripMargin)
    val root = paths.cncfHome.resolve("textus-control-center")
    val token = root.resolve("credentials").resolve("launcher-registration.token")
    _write(token, "standalone-token\n")
    Files.setPosixFilePermissions(token, java.util.Set.of(java.nio.file.attribute.PosixFilePermission.OWNER_READ, java.nio.file.attribute.PosixFilePermission.OWNER_WRITE))
    _write(root.resolve("standalone-locator.yaml"),
      """schemaVersion: 1
        |profile: standalone
        |scopeId: scope-test
        |installationId: standalone-test
        |endpoint: http://127.0.0.1:18013/rest/v1/textus-control-center/subsystem-inventory
        |credentialRef: credentials/launcher-registration.token
        |timeout: 2s
        |heartbeatInterval: 30s
        |hostLabel: standalone-test
        |""".stripMargin)
    val reporter = FakeTextusControlCenterRegistrationReporter()
    val invoker = FakeInvoker()
    val launcher = new TextusLauncher(paths, FakeResolver(), invoker, registrationreporter = reporter)

    When("the canonical target-first server command runs without inline registration configuration")
    val code = launcher.run(Vector("textus-registration", "server", "--textus.server.port=18014"))

    Then("the locator credential supplies one Textus lifecycle session")
    _assert_equals(code, 0)
    _assert_equals(reporter.starts.size, 1)
    _assert_equals(reporter.starts.head._1.target, "textus-registration")
    _assert_equals(reporter.starts.head._2, Some("standalone-token"))
    _assert_equals(reporter.configs.head.baseUrl, "http://127.0.0.1:18014")
    _assert_equals(reporter.closes, 1)

    When("the shared credential is no longer owner-readable and owner-writable only")
    Files.setPosixFilePermissions(token, java.util.Set.of(java.nio.file.attribute.PosixFilePermission.OWNER_READ))
    val invalidlocatorcode = launcher.run(Vector("textus-registration", "server"))

    Then("the invalid locator is ignored without changing server startup")
    _assert_equals(invalidlocatorcode, 0)
    _assert_equals(reporter.starts.size, 1)

    When("an explicit registration disable is configured")
    _write(paths.cwd.resolve(".textus").resolve("config.yaml"),
      s"""repositories:
         |  car:
         |    - $carrepo
         |textus-control-center:
         |  registration:
         |    enabled: false
         |""".stripMargin)
    val disabledlauncher = new TextusLauncher(paths, FakeResolver(), FakeInvoker(), registrationreporter = reporter)
    val disabledcode = disabledlauncher.run(Vector("textus-registration", "server"))

    Then("the machine locator is not used")
    _assert_equals(disabledcode, 0)
    _assert_equals(reporter.starts.size, 1)
  }

  def textusControlCenterRegistrationHttpLifecycle(): Unit = {
    Given("a reachable Textus Control Center automatic REST endpoint")
    val requests = new ConcurrentLinkedQueue[(String, String)]()
    val rejectnextheartbeat = new java.util.concurrent.atomic.AtomicBoolean(false)
    val server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/", new HttpHandler {
      override def handle(exchange: HttpExchange): Unit = {
        val path = exchange.getRequestURI.toString
        requests.add(path -> exchange.getRequestHeaders.getFirst("Authorization"))
        val status =
          if (path.contains("heartbeat-subsystem") && rejectnextheartbeat.compareAndSet(true, false)) 503
          else 204
        exchange.sendResponseHeaders(status, -1)
        exchange.close()
      }
    })
    server.start()
    try {
      val config = TextusControlCenterRegistrationConfig(
        endpoint = s"http://127.0.0.1:${server.getAddress.getPort}/rest/v1/textus-control-center/subsystem-inventory",
        tokenEnv = "TEXTUS_ADMIN_REGISTRATION_TOKEN",
        timeout = java.time.Duration.ofSeconds(1),
        heartbeatInterval = java.time.Duration.ofSeconds(30),
        hostLabel = "acceptance",
        baseUrl = "https://subsystem.example.test"
      )
      val report = TextusControlCenterRegistrationReport(
        instanceId = "textus-registration-http-spec",
        target = "textus-registration",
        artifactId = Some("textus-registration"),
        executionMode = "artifact",
        developmentDirectory = None,
        subsystemName = Some("textus-registration"),
        subsystemVersion = Some("0.1.0"),
        runtimeVersion = "0.5.0",
        startedAt = java.time.Instant.parse("2026-07-18T00:00:00Z")
      )

      When("the reporter starts and closes one server registration session")
      val session = TextusControlCenterRegistrationReporter.System.start(config, report, Some("test-token"))
      session.close()
      session.close()

      Then("it sends one register and one deregister operation with the configured bearer credential")
      _assert_equals(requests.size, 2)
      requests.iterator.asScala.map(_._1).exists(_.contains("register-subsystem")) shouldBe true
      requests.iterator.asScala.map(_._1).exists(_.contains("deregister-subsystem")) shouldBe true
      requests.iterator.asScala.forall(_._2 == "Bearer test-token") shouldBe true
      requests.iterator.asScala.forall { case (path, _) => path.contains("instanceId=textus-registration-http-spec") } shouldBe true
      requests.iterator.asScala.forall { case (path, _) => path.contains("artifactId=textus-registration") } shouldBe true
      requests.iterator.asScala.forall { case (path, _) => path.contains("?protocolVersion=1&instanceId=") } shouldBe true

      Given("registration without an explicit public base URL")
      requests.clear()
      val propertykey = "textus.server.bound-base-url"
      sys.props.remove(propertykey)

      When("CNCF publishes the endpoint after the server has bound")
      val dynamicsession = TextusControlCenterRegistrationReporter.System.start(config.copy(baseUrl = ""), report, Some("test-token"))
      sys.props.update(propertykey, "http://127.0.0.1:38001")
      val deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(2)
      while (requests.size < 1 && System.nanoTime() < deadline)
        Thread.sleep(10L)
      dynamicsession.close()
      val closedeadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(2)
      while (requests.size < 2 && System.nanoTime() < closedeadline)
        Thread.sleep(10L)
      sys.props.remove(propertykey)

      Then("registration uses the bound endpoint rather than a guessed default port")
      _assert_equals(requests.size, 2)
      requests.iterator.asScala.forall(_._1.contains("baseUrl=http%3A%2F%2F127.0.0.1%3A38001")) shouldBe true

      Given("a registered server whose heartbeat has one transient communication failure")
      requests.clear()
      rejectnextheartbeat.set(true)

      When("the following heartbeat interval reaches Control Center again")
      val recoveringsession = TextusControlCenterRegistrationReporter.System.start(
        config.copy(heartbeatInterval = java.time.Duration.ofMillis(20)),
        report,
        Some("test-token")
      )
      val recoverydeadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(2)
      while (requests.iterator.asScala.count(_._1.contains("heartbeat-subsystem")) < 2 && System.nanoTime() < recoverydeadline)
        Thread.sleep(10L)
      recoveringsession.close()
      val recoveryrequests = requests.iterator.asScala.map(_._1).toVector

      Then("the launcher retains running state and retries heartbeat without registering as starting again")
      recoveryrequests.count(_.contains("/register-subsystem?")) shouldBe 1
      recoveryrequests.count(_.contains("/heartbeat-subsystem?")) should be >= 2
      recoveryrequests.count(_.contains("/deregister-subsystem?")) shouldBe 1

      And("the previous canonical textus-admin key remains readable during migration")
      val legacyvalues = LauncherConfigParser.parse(
        Path.of("legacy-launcher.yaml"),
        """textus-admin:
          |  registration:
          |    enabled: true
          |    endpoint: https://admin.example.test/inventory
          |    token-env: TOKEN
          |    host-label: legacy
          |""".stripMargin
      )
      TextusControlCenterRegistrationConfig.fromParsed(legacyvalues).map(_.hostLabel) shouldBe Some("legacy")
    } finally {
      sys.props.remove("textus.server.bound-base-url")
      server.stop(0)
    }
  }

  def lifecycleSupervisorDelegateLifecycle(): Unit = {
    Given("a local CNCF launcher supervisor that owns lifecycle requests")
    val requests = new ConcurrentLinkedQueue[(String, String, String)]()
    val server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/v1/lifecycle-requests", new HttpHandler {
      override def handle(exchange: HttpExchange): Unit = {
        val body = new String(exchange.getRequestBody.readAllBytes(), StandardCharsets.UTF_8)
        requests.add((exchange.getRequestMethod, exchange.getRequestURI.toString, exchange.getRequestHeaders.getFirst("Authorization") + "|" + body))
        val response = """{"requestId":"delegated-request","state":"accepted","instanceId":"supervisor-instance"}""".getBytes(StandardCharsets.UTF_8)
        exchange.getResponseHeaders.set("Content-Type", "application/json")
        exchange.sendResponseHeaders(200, response.length)
        val output = exchange.getResponseBody
        try output.write(response) finally output.close()
      }
    })
    server.createContext("/v1/lifecycle-requests/delegated-request", new HttpHandler {
      override def handle(exchange: HttpExchange): Unit = {
        requests.add((exchange.getRequestMethod, exchange.getRequestURI.toString, exchange.getRequestHeaders.getFirst("Authorization") + "|"))
        val response = """{"requestId":"delegated-request","state":"accepted","instanceId":"supervisor-instance"}""".getBytes(StandardCharsets.UTF_8)
        exchange.getResponseHeaders.set("Content-Type", "application/json")
        exchange.sendResponseHeaders(200, response.length)
        val output = exchange.getResponseBody
        try output.write(response) finally output.close()
      }
    })
    server.start()
    try {
      val endpoint = java.net.URI.create(s"http://127.0.0.1:${server.getAddress.getPort}")
      val requestjson = """{"requestId":"delegated-request","idempotencyKey":"key","artifactId":"textus-registration","action":"start","operatorSubjectId":"operator","deadlineAt":"2026-07-22T00:05:00Z"}"""

      When("Textus launcher delegates one lifecycle submission and later lookup to the shared supervisor")
      val submitted = LifecycleSupervisorDelegate.System.submit(endpoint, "supervisor-token", requestjson, java.time.Duration.ofSeconds(1))
      val lookedup = LifecycleSupervisorDelegate.System.lookup(endpoint, "supervisor-token", "delegated-request", java.time.Duration.ofSeconds(1))

      Then("it uses only the authenticated local protocol and preserves the supervisor result without creating process authority")
      submitted shouldBe Right("""{"requestId":"delegated-request","state":"accepted","instanceId":"supervisor-instance"}""")
      lookedup shouldBe Right("""{"requestId":"delegated-request","state":"accepted","instanceId":"supervisor-instance"}""")
      requests.asScala.toVector.map(_._1) shouldBe Vector("POST", "GET")
      requests.asScala.forall(_._2.startsWith("/v1/lifecycle-requests")) shouldBe true
      requests.asScala.forall(_._3.startsWith("Bearer supervisor-token|")) shouldBe true

      And("a non-loopback endpoint and malformed request identity do not cause a network request")
      LifecycleSupervisorDelegate.System.submit(java.net.URI.create("https://supervisor.example.test"), "supervisor-token", requestjson, java.time.Duration.ofSeconds(1)) shouldBe Left("supervisor-protocol-unavailable")
      LifecycleSupervisorDelegate.System.lookup(endpoint, "supervisor-token", " ", java.time.Duration.ofSeconds(1)) shouldBe Left("supervisor-request-invalid")
      requests.size shouldBe 2
    } finally server.stop(0)
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

  def noCncfRuntimeLibraryDependencies(): Unit = {
    val build = Files.readString(Path.of("build.sbt"))
    build should not include "goldenport-cncf"
    build should include("goldenport-launcher-core")
  }

  def componentRepositoryCommandParser(): Unit = {
    val list = TextusCommandParser.parse(Vector("repository", "list", "--kind", "car", "--source=https://repo.example/repository"))
      .asInstanceOf[TextusCommand.Repository.ListArtifacts]
    list.kind shouldBe Some(ArtifactKind.Car)
    list.source shouldBe Some("https://repo.example/repository")

    val show = TextusCommandParser.parse(Vector("repository", "show", "textus-blog", "--kind=sar"))
      .asInstanceOf[TextusCommand.Repository.Show]
    show.artifactId shouldBe "textus-blog"
    show.kind shouldBe Some(ArtifactKind.Sar)

    val refresh = TextusCommandParser.parse(Vector("repository", "refresh", "https://repo.example/repository"))
      .asInstanceOf[TextusCommand.Repository.Refresh]
    refresh.source shouldBe Some("https://repo.example/repository")
  }

  def componentRepositoryLocalPrecedenceAndConflictDiagnostics(): Unit = _with_temp_paths { paths =>
    val first = paths.cwd.resolve("first/repository")
    val second = paths.cwd.resolve("second/repository")
    val remote = "https://repo.example/repository"
    _write(first.resolve("catalog/index.json"), _component_repository_index("car", "textus-blog", "1.0.0"))
    _write(second.resolve("catalog/index.json"), _component_repository_index("car", "textus-blog", "1.1.0"))
    val client = new RecordingComponentRepositoryHttpClient(Map(
      s"$remote/catalog/index.json" -> _component_repository_index("car", "textus-blog", "2.0.0")
    ))
    val discovery = new ComponentRepositoryDiscovery(paths, client, () => java.time.Instant.parse("2026-07-21T00:00:00Z"))
    discovery.refresh(LauncherConfig(carRepositories = Vector(s"$remote/car")), None)

    val config = LauncherConfig(carRepositories = Vector(first.resolve("car").toString, second.resolve("car").toString, s"$remote/car"))
    val result = discovery.list(config, None, None)

    result.artifacts.map(_.entry.recommended) shouldBe Vector(Some("1.0.0"))
    result.artifacts.head.origin shouldBe "local"
    result.artifacts.head.render should not include paths.cwd.toString
    result.diagnostics.exists(_.contains("equal precedence")) shouldBe true
    client.requests shouldBe Vector(s"$remote/catalog/index.json")
  }

  def componentRepositoryRefreshIsBoundedAndPreservesStaleCache(): Unit = _with_temp_paths { paths =>
    val remote = "https://repo.example/repository"
    val client = new RecordingComponentRepositoryHttpClient(Map(
      s"$remote/catalog/index.json" -> _component_repository_index("sar", "sample-app", "1.0.0")
    ))
    val discovery = new ComponentRepositoryDiscovery(paths, client, () => java.time.Instant.parse("2026-07-21T00:00:00Z"))
    val config = LauncherConfig(sarRepositories = Vector(s"$remote/sar"))

    discovery.refresh(config, None).failures shouldBe empty
    client.responses = Map(s"$remote/catalog/index.json" -> "{ malformed")
    val failed = discovery.refresh(config, None)
    val listed = discovery.list(config, None, None)

    failed.failures should have size 1
    listed.artifacts.map(_.entry.artifactId) shouldBe Vector("sample-app")
    listed.diagnostics.exists(_.contains("stale component repository cache")) shouldBe true
    client.requests.distinct shouldBe Vector(s"$remote/catalog/index.json")
    client.requests.exists(_.endsWith(".car")) shouldBe false
    client.requests.exists(_.endsWith(".sar")) shouldBe false
    ComponentRepositoryDiscovery.safeSource("https://user:secret@repo.example/repository?token=private") shouldBe "https://repo.example/repository"
  }

  def componentRepositoryShowValidatesDetailedCatalog(): Unit = _with_temp_paths { paths =>
    val repository = paths.cwd.resolve("repository")
    _write(repository.resolve("catalog/index.json"), _component_repository_index("car", "textus-blog", "1.0.0"))
    _write(repository.resolve("catalog/car/textus-blog.yaml"), _artifact_catalog_text("1.0.0", "0.4.0", Vector("0.4.0")))
    val discovery = new ComponentRepositoryDiscovery(paths, new RecordingComponentRepositoryHttpClient(Map.empty))

    val result = discovery.show(LauncherConfig(carRepositories = Vector(repository.resolve("car").toString)), "textus-blog", Some(ArtifactKind.Car), None)

    result.artifact.entry.identity shouldBe ("car" -> "textus-blog")
    result.artifact.renderDetailed should include("recommended: 1.0.0")
  }

  def componentRepositoryShowValidatesJsonDetailedCatalog(): Unit = _with_temp_paths { paths =>
    val repository = paths.cwd.resolve("repository")
    _write(repository.resolve("catalog/index.json"), _component_repository_index("sar", "sample-app", "1.2.0", "json"))
    _write(
      repository.resolve("catalog/sar/sample-app.json"),
      """{
        |  "kind": "sar",
        |  "artifactId": "sample-app",
        |  "status": "active",
        |  "recommended": "1.2.0",
        |  "latestStable": "1.2.0"
        |}
        |""".stripMargin
    )
    val discovery = new ComponentRepositoryDiscovery(paths, new RecordingComponentRepositoryHttpClient(Map.empty))

    val result = discovery.show(LauncherConfig(sarRepositories = Vector(repository.resolve("sar").toString)), "sample-app", Some(ArtifactKind.Sar), None)

    result.artifact.entry.identity shouldBe ("sar" -> "sample-app")
    result.artifact.entry.catalog shouldBe "sar/sample-app.json"
  }

  def componentRepositoryShowPreservesValidatedDetailCache(): Unit = _with_temp_paths { paths =>
    val remote = "https://repo.example/repository"
    val indexurl = s"$remote/catalog/index.json"
    val detailurl = s"$remote/catalog/car/textus-blog.yaml"
    val client = new RecordingComponentRepositoryHttpClient(Map(
      indexurl -> _component_repository_index("car", "textus-blog", "1.0.0"),
      detailurl -> _artifact_catalog_text("1.0.0", "0.4.0", Vector("0.4.0"))
    ))
    val discovery = new ComponentRepositoryDiscovery(paths, client)
    val config = LauncherConfig(carRepositories = Vector(s"$remote/car"))
    discovery.refresh(config, None)
    discovery.show(config, "textus-blog", Some(ArtifactKind.Car), None).diagnostics shouldBe empty
    client.responses = Map(detailurl -> _artifact_catalog_text("9.9.9", "0.4.0", Vector("0.4.0")))

    val stale = discovery.show(config, "textus-blog", Some(ArtifactKind.Car), None)
    client.responses = Map.empty
    val offline = discovery.show(config, "textus-blog", Some(ArtifactKind.Car), None)

    stale.diagnostics.exists(_.contains("stale detailed catalog cache")) shouldBe true
    offline.diagnostics.exists(_.contains("stale detailed catalog cache")) shouldBe true
    stale.artifact.entry.recommended shouldBe Some("1.0.0")
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

  private def _entries(paths: LauncherPaths): Vector[LocalServerEvidenceEntry] = {
    import LocalServerEvidenceSnapshot.given
    import io.circe.parser.decode
    decode[LocalServerEvidenceSnapshot](Files.readString(paths.serverEvidence)).toOption.map(_.entries).getOrElse(Vector.empty)
  }

  private def _local_evidence_report(instanceid: String): TextusControlCenterRegistrationReport =
    TextusControlCenterRegistrationReport(
      instanceId = instanceid,
      target = instanceid,
      artifactId = Some(instanceid),
      executionMode = "artifact",
      developmentDirectory = None,
      subsystemName = None,
      subsystemVersion = None,
      runtimeVersion = "0.5.0-SNAPSHOT",
      startedAt = java.time.Instant.now()
    )

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

  private def _component_repository_index(kind: String, artifactid: String, version: String, extension: String = "yaml"): String =
    s"""{
       |  "schemaVersion": "cncf.component-repository-index.v1",
       |  "generatedAt": "2026-07-21T00:00:00Z",
       |  "artifacts": [{
       |    "kind": "$kind",
       |    "artifactId": "$artifactid",
       |    "catalog": "$kind/$artifactid.$extension",
       |    "status": "active",
       |    "recommended": "$version",
       |    "latestStable": "$version"
       |  }]
       |}
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

final class RecordingComponentRepositoryHttpClient(var responses: Map[String, String]) extends ComponentRepositoryHttpClient {
  var requests: Vector[String] = Vector.empty

  def get(url: String): String = {
    requests :+= url
    responses.getOrElse(url, throw TextusException(s"missing test response: $url"))
  }
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

final class FakeTextusControlCenterRegistrationReporter extends TextusControlCenterRegistrationReporter {
  var starts: Vector[(TextusControlCenterRegistrationReport, Option[String])] = Vector.empty
  var configs: Vector[TextusControlCenterRegistrationConfig] = Vector.empty
  var closes: Int = 0

  def start(
    config: TextusControlCenterRegistrationConfig,
    report: TextusControlCenterRegistrationReport,
    token: Option[String]
  ): TextusControlCenterRegistrationSession = {
    starts :+= report -> token
    configs :+= config
    new TextusControlCenterRegistrationSession {
      def close(): Unit = closes += 1
    }
  }
}

object FakeTextusControlCenterRegistrationReporter {
  def apply(): FakeTextusControlCenterRegistrationReporter = new FakeTextusControlCenterRegistrationReporter()
}

final class ThrowingTextusControlCenterRegistrationReporter extends TextusControlCenterRegistrationReporter {
  def start(
    config: TextusControlCenterRegistrationConfig,
    report: TextusControlCenterRegistrationReport,
    token: Option[String]
  ): TextusControlCenterRegistrationSession =
    throw TextusException("simulated Textus Control Center outage")
}

object FakeInvoker {
  def apply(): FakeInvoker = new FakeInvoker()
}

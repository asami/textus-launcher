package textus.launcher

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import io.circe.Json
import io.circe.parser.parse
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Oct. 17, 2026
 * @version Oct. 17, 2026
 */
final class LocalCarActivationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  private val _samples = for {
    length <- Gen.choose(4, 16)
    letters <- Gen.listOfN(length, Gen.alphaLowerChar)
    major <- Gen.choose(1, 99)
    segment <- Gen.oneOf("warehouse", "warehouse with spaces", "warehouse-λ")
  } yield ("textus-" + letters.mkString, major, segment)

  "Local archive resolution" should {
    "retain metadata provenance" which {
      "keeps explicit and metadata-latest selected CAR paths across generated identities and repository paths" in {
        _check_cases("LC1") { sample =>
          val (name, major, segment) = sample
          _with_paths { paths =>
            Given("a local metadata repository with two versions and an artifact name distinct from a native qualified Component ID")
            val repository = paths.cwd.resolve(segment).resolve("car")
            val older = s"$major.1.0"
            val latest = s"$major.2.0"
            val first = _archive(repository, name, older, "car")
            val second = _archive(repository, name, latest, "car")
            _write(repository.resolve(name).resolve("maven-metadata.xml"),
              s"<metadata><versioning><latest>$latest</latest><versions><version>$older</version><version>$latest</version></versions></versioning></metadata>")
            val relative = Path.of("").toAbsolutePath.relativize(repository).resolve(".")
            val config = LauncherConfig(carRepositories = Vector(relative.toString))
            When("the real resolver selects an explicit version and then the metadata latest version")
            val explicit = ArtifactResolver().resolve(ArtifactSelector(name, Some(older), ArtifactKind.Car), config)
            val current = ArtifactResolver().resolve(ArtifactSelector(name, None, ArtifactKind.Car), config)
            Then("each selected regular archive is retained as its exact normalized absolute path with unchanged artifact identity")
            explicit.localArchive shouldBe Some(first.toAbsolutePath.normalize)
            current.localArchive shouldBe Some(second.toAbsolutePath.normalize)
            explicit.selector shouldBe ArtifactSelector(name, Some(older), ArtifactKind.Car)
            current.selector shouldBe ArtifactSelector(name, Some(latest), ArtifactKind.Car)
            current.runtimeRequirements shouldBe empty
          }
        }
      }
    }

    "retain catalog selection and compatibility requirements" which {
      "keeps generated default and authored file paths with component and dependency runtime requirements" in {
        _check_cases("LC2") { sample =>
          val (name, major, segment) = sample
          _with_paths { paths =>
            Given("a catalog version with runtime constraints and a dependency catalog")
            val repository = paths.cwd.resolve(segment).resolve("car")
            val version = s"$major.3.0"
            val dependency = name + "-dependency"
            val conventional = _archive(repository, name, version, "car")
            val overridepath = repository.getParent.resolve(s"custom archives/$name selected.car")
            _write(overridepath, "CAR fixture; native identity belongs to its descriptor")
            val catalogpath = repository.getParent.resolve(s"catalog/car/$name.yaml")
            val catalog = s"""recommended: $version
              |dependencies:
              |  car:
              |    - $dependency:1.0.0
              |versions:
              |  - version: $version
              |    status: active
              |    runtime:
              |      cncf:
              |        minimum: 0.4.0
              |        maximum: 0.9.0
              |        excluded: [0.6.0]
              |        tested: [0.5.0]
              |""".stripMargin
            _write(repository.getParent.resolve(s"catalog/car/$dependency.yaml"),
              "versions:\n  - version: 1.0.0\n    runtime:\n      cncf:\n        minimum: 0.5.0\n")
            val config = LauncherConfig(carRepositories = Vector(repository.toString))
            _write(catalogpath, catalog)
            When("the catalog selects its conventional archive")
            val defaultresolved = ArtifactResolver().resolve(ArtifactSelector(name, None, ArtifactKind.Car), config)
            Then("the default archive is retained")
            defaultresolved.localArchive shouldBe Some(conventional.toAbsolutePath.normalize)
            Given("the same catalog now explicitly selects a different regular file")
            _write(catalogpath, catalog.replace("    status: active", s"    status: active\n    file: repository/custom archives/$name selected.car"))
            When("the real resolver reads the authored file override")
            val resolved = ArtifactResolver().resolve(ArtifactSelector(name, None, ArtifactKind.Car), config)
            Then("the override wins and all authored component and dependency compatibility requirements survive")
            resolved.localArchive shouldBe Some(overridepath.toAbsolutePath.normalize)
            resolved.selector shouldBe ArtifactSelector(name, Some(version), ArtifactKind.Car)
            resolved.runtimeRequirements shouldBe Vector(
              RuntimeRequirement(Some("0.4.0"), Some("0.9.0"), Vector("0.6.0"), Vector("0.5.0"), name),
              RuntimeRequirement(minimum = Some("0.5.0"), source = dependency))
            RuntimeVersionSelection.select(Some("0.5.0"), "0.5.0", resolved.runtimeRequirements, None,
              RuntimeSelectionPolicy.CurrentCompatible, RuntimeNoCompatiblePolicy.Error) shouldBe "0.5.0"
            intercept[TextusException] {
              RuntimeVersionSelection.select(Some("0.1.0"), "0.5.0", resolved.runtimeRequirements, None,
                RuntimeSelectionPolicy.CurrentCompatible, RuntimeNoCompatiblePolicy.Error)
            }.getMessage should include("not compatible")
          }
        }
      }
    }

    "preserve repository and rejection boundaries" which {
      "retains precedence, missing-archive fallback and diagnostics, and disabled or missing catalog rejection" in {
        _with_paths { paths =>
          Given("two local repositories containing the same artifact and version")
          val first = paths.cwd.resolve("first/car")
          val second = paths.cwd.resolve("second/car")
          val firstarchive = _archive(first, "textus-precedence", "1.0.0", "car")
          val secondarchive = _archive(second, "textus-precedence", "1.0.0", "car")
          val selector = ArtifactSelector("textus-precedence", Some("1.0.0"), ArtifactKind.Car)
          val config = LauncherConfig(carRepositories = Vector(first.toString, second.toString))
          When("both repositories can satisfy the same request")
          val selected = ArtifactResolver().resolve(selector, config)
          Then("the first configured repository owns the selected path")
          selected.localArchive shouldBe Some(firstarchive)
          Given("the first catalog selects a missing authored archive while metadata still has a regular archive")
          val catalogpath = first.getParent.resolve("catalog/car/textus-precedence.yaml")
          _write(catalogpath, "versions:\n  - version: 1.0.0\n    file: repository/missing.car\n")
          When("the resolver continues to the second repository")
          val fallback = ArtifactResolver().resolve(selector, config)
          Then("the missing selected file does not fall back to metadata in its own repository")
          fallback.localArchive shouldBe Some(secondarchive)
          When("no later repository supplies the archive")
          val missing = intercept[TextusException] {
            ArtifactResolver().resolve(selector, config.copy(carRepositories = Vector(first.toString)))
          }
          Then("the original selected-archive diagnostic is retained")
          missing.getMessage should include("local catalog points to a missing archive: textus-precedence:1.0.0")
          Given("the catalog disables its only version")
          _write(catalogpath, "versions:\n  - version: 1.0.0\n    status: disabled\n")
          When("the requested archive exists but its catalog version is disabled")
          val disabled = intercept[TextusException] { ArtifactResolver().resolve(selector, config) }
          Then("disabled catalog selection still rejects without consulting metadata or later repositories")
          disabled.getMessage should include("does not contain an enabled version")
          Given("the first catalog has no versions at all")
          _write(catalogpath, "versions: []\n")
          When("the requested version is absent from the catalog")
          val absent = intercept[TextusException] {
            ArtifactResolver().resolve(selector, config.copy(carRepositories = Vector(first.toString)))
          }
          Then("the missing catalog-version diagnostic survives even though its metadata archive exists")
          absent.getMessage should include("local catalog does not contain textus-precedence:1.0.0")
        }
      }
    }
  }

  "Local CAR runtime activation" should {
    "preserve the native file contract and artifact observation identity" which {
      "records exact production CAR arguments and unchanged SAR arguments for generated names, versions and paths" in {
        _check_cases("LC3") { sample =>
          val (name, major, segment) = sample
          _with_paths { paths =>
            Given("real CAR and SAR repositories, an offline runtime catalog and recording invocation and registration doubles")
            val repository = paths.cwd.resolve(segment)
            val version = s"$major.4.0"
            val archive = _archive(repository.resolve("car"), name, version, "car")
            _archive(repository.resolve("sar"), name + "-system", version, "sar")
            _write(paths.runtimeCatalog, "schemaVersion: 1\nversions:\n  - version: 0.5.0\n    status: active\n")
            _write(paths.projectConfig, s"""runtime:
              |  version: 0.5.0
              |repositories:
              |  car:
              |    - ${repository.resolve("car")}
              |  sar:
              |    - ${repository.resolve("sar")}
              |textus-control-center:
              |  registration:
              |    enabled: true
              |    endpoint: http://127.0.0.1:1
              |    token-env: LOCAL_CAR_TOKEN
              |    host-label: local-car-fixture
              |""".stripMargin)
            val invoker = new RecordingInvoker
            val reporter = FakeTextusControlCenterRegistrationReporter()
            val launcher = new TextusLauncher(paths, FakeResolver(), invoker,
              environment = Map("LOCAL_CAR_TOKEN" -> "fixture-token"), registrationreporter = reporter)
            val ordinary = Vector("--custom=kept", "payload")
            val passthrough = Vector("--launcher-home", "runtime-value", "--other-option=unchanged")
            val config = LauncherConfig.load(paths, Vector.empty, Map.empty[String, String])
            val repositoryargs = (config.carRepositories ++ config.sarRepositories).map(value => s"--repository-dir=$value")
            When("the public launcher runs the locally resolved CAR server with ordinary and passthrough arguments")
            val exit = launcher.run(Vector(s"$name.car:$version", "server") ++ ordinary ++ Vector("--") ++ passthrough)
            Then("only the exact native file activation is inserted and all repository, mode and caller arguments retain order and spelling")
            exit shouldBe 0
            invoker.arguments shouldBe repositoryargs ++ Vector(s"--textus.component.file=$archive", "server") ++ ordinary ++ passthrough
            invoker.arguments.exists(_.startsWith("--textus.component=")) shouldBe false
            invoker.arguments.exists(_.startsWith("--textus.component.version=")) shouldBe false
            reporter.starts should have size 1
            val report = reporter.starts.head._1
            report.target shouldBe name
            report.artifactId shouldBe Some(name)
            report.subsystemVersion shouldBe Some(version)
            reporter.closes shouldBe 1
            val entries = parse(Files.readString(paths.serverEvidence)).toOption.get.hcursor.get[Vector[Json]]("entries").toOption.get
            entries should have size 1
            entries.head.hcursor.get[String]("target").toOption shouldBe Some(name)
            entries.head.hcursor.get[Option[String]]("subsystemVersion").toOption.flatten shouldBe Some(version)
            entries.head.hcursor.get[Option[String]]("stoppedAt").toOption.flatten should not be empty
            Given("the same launcher and repositories contain a local SAR")
            When("the public launcher runs the SAR command")
            val sarcode = launcher.run(Vector(s"$name-system.sar:$version", "command", "system.status"))
            Then("SAR activation continues to use the versioned subsystem selector")
            sarcode shouldBe 0
            invoker.arguments shouldBe repositoryargs ++ Vector(s"--textus.subsystem=$name-system-$version", "command", "system.status")
          }
        }
      }
    }

    "retain source and nonlocal argument compatibility" which {
      "defaults old positional constructors to no archive and retains selector and optional-version arguments" in {
        Given("pre-existing positional ResolvedArtifact constructors with no local archive")
        val launcher = new TextusLauncher()
        val config = LauncherConfig(carRepositories = Vector("https://example.invalid/car"))
        val method = classOf[TextusLauncher].getDeclaredMethod("_cncf_args", classOf[TextusCommand.Execute], classOf[ResolvedArtifact], classOf[LauncherConfig])
        method.setAccessible(true)
        Vector(None, Some("3.2.1")).foreach { version =>
          val selector = ArtifactSelector("textus-remote", version, ArtifactKind.Car)
          val artifact = ResolvedArtifact(selector, ArtifactKind.Car, Vector.empty)
          val command = TextusCommand.Execute("command", selector, Vector("operation", "x=1"), None, None, None, None, Vector("--kept"))
          When("the existing native argument conversion receives a resolved CAR without a local path")
          val arguments = method.invoke(launcher, command, artifact, config).asInstanceOf[Vector[String]]
          Then("the compatibility default and original selector/version argument order are preserved without remote access")
          artifact.localArchive shouldBe None
          arguments shouldBe Vector("--repository-dir=https://example.invalid/car", "--textus.component=textus-remote") ++
            version.toVector.map(value => s"--textus.component.version=$value") ++ Vector("command", "operation", "x=1", "--kept")
        }
      }
    }
  }

  private def _check_cases(label: String)(check: ((String, Int, String)) => Unit): Unit = {
    val result = Test.check(Test.Parameters.default.withMinSuccessfulTests(50).withWorkers(1),
      Prop.forAll(_samples) { sample => check(sample); true })
    Then("at least fifty generated cases succeed without discards")
    info(s"$label successful=${result.succeeded} discarded=${result.discarded}")
    withClue(s"$label status=${result.status}: ") { result.passed shouldBe true }
    result.succeeded should be >= 50
    result.discarded shouldBe 0
  }

  private def _archive(repository: Path, name: String, version: String, suffix: String): Path = {
    val path = repository.resolve(name).resolve(version).resolve(s"$name-$version.$suffix").toAbsolutePath.normalize
    _write(path, "archive fixture")
    path
  }

  private def _with_paths[A](run: LauncherPaths => A): A = {
    val parent = Files.createDirectories(Path.of("target", "local-car-activation")).toAbsolutePath.normalize
    val directory = Files.createTempDirectory(parent, "case-")
    try run(LauncherPaths(Files.createDirectories(directory.resolve("home")), Files.createDirectories(directory.resolve("work"))))
    finally {
      val files = Files.walk(directory)
      try files.iterator().asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.delete)
      finally files.close()
    }
  }

  private def _write(path: Path, value: String): Unit = {
    Files.createDirectories(path.getParent)
    Files.writeString(path, value)
    ()
  }

  private final class RecordingInvoker extends CncfInvoker {
    private var _arguments = Vector.empty[String]
    def arguments: Vector[String] = _arguments
    override def invoke(classpath: Vector[Path], args: Vector[String]): Int = {
      _arguments = args
      0
    }
  }
}

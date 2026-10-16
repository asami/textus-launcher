package textus.launcher

import java.nio.file.{Files, Path}
import java.util.UUID
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
final class SupervisorInstanceCorrelationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  private val _identities = for {
    supervisor <- Gen.listOfN(32, Gen.oneOf("0123456789abcdef".toVector)).map(_.mkString)
    legacy <- Gen.uuid.map(_.toString)
    opaque <- Gen.nonEmptyListOf(Gen.alphaNumChar).map(chars => "invocation:" + chars.take(20).mkString)
  } yield Vector(supervisor, legacy, opaque)
  private val _unrelated = Vector("--textus.server.port=38010", "--custom=retained", "payload",
    "--textus.control-center.registration-instance-id-extra=keep")

  "Textus Supervisor invocation correlation" should {
    "retain supplied opaque identities" which {
      "correlates at least fifty generated cases of each identity shape in arguments and passthrough" in {
        Given("32hex Supervisor identities, legacy UUIDs and other opaque identities with a fake runtime and registration reporter")
        When("each generated identity crosses public Launcher.run in ordinary arguments and after the passthrough delimiter")
        val result = Test.check(Test.Parameters.default.withMinSuccessfulTests(50).withWorkers(1),
          Prop.forAll(_identities) { identities =>
            identities.foreach { instanceid =>
              Vector(false, true).foreach { passthrough =>
                _with_paths { paths =>
                  val observed = _exercise(paths, Some("  " + instanceid + "  "), passthrough)
                  Then("report and durable evidence reuse the exact trimmed opaque identity")
                  observed shouldBe instanceid
                }
              }
            }
            true
          })
        Then("at least fifty complete cases cover every identity shape and argument location without discards")
        info(s"IC1 textus successful=${result.succeeded} discarded=${result.discarded}")
        withClue(s"status=${result.status}: ") { result.passed shouldBe true }
        result.succeeded should be >= 50
        result.discarded shouldBe 0
      }
    }

    "allocate an identity when metadata is absent or empty" which {
      "creates distinct fresh UUIDs while withholding empty internal options in both argument locations" in {
        Given("absent, empty and whitespace-only internal metadata on independent canonical invocations")
        val identities = Vector(false, true).flatMap { passthrough =>
          Vector(None, Some(""), Some("  ")).map { supplied =>
            _with_paths { paths => _exercise(paths, supplied, passthrough) }
          }
        }
        Then("each report contains a fresh UUID rather than an empty identity")
        identities.foreach { instanceid => UUID.fromString(instanceid).toString shouldBe instanceid }
        identities.distinct should have size identities.size
      }
    }
  }

  private def _exercise(paths: LauncherPaths, supplied: Option[String], passthrough: Boolean): String = {
    Given("a target-owned local CAR repository, cached runtime catalog and recording public test doubles")
    val repository = paths.cwd.resolve("repo/car")
    _write(repository.resolve("correlation-component/1.0.0/correlation-component-1.0.0.car"), "")
    _write(paths.runtimeCatalog, "schemaVersion: 1\nversions:\n  - version: 0.4.12\n    status: active\n")
    _write(paths.projectConfig, s"""runtime:
      |  version: 0.4.12
      |repositories:
      |  car:
      |    - $repository
      |textus-control-center:
      |  registration:
      |    enabled: true
      |    endpoint: http://127.0.0.1:1
      |    token-env: CORRELATION_TOKEN
      |    host-label: correlation-fixture
      |""".stripMargin)
    val resolver = FakeResolver()
    val invoker = FakeInvoker()
    val reporter = FakeTextusControlCenterRegistrationReporter()
    val launcher = new TextusLauncher(paths, resolver, invoker,
      environment = Map("CORRELATION_TOKEN" -> "fixture-token"), registrationreporter = reporter)
    val metadata = supplied.toVector.map(value => "--textus.control-center.registration-instance-id=" + value)
    val arguments = Vector("correlation-component.car:1.0.0", "server") ++ _unrelated ++
      (if (passthrough) Vector("--") ++ metadata else metadata)
    When("the canonical artifact server invocation completes through Launcher.run")
    val exit = launcher.run(arguments)
    val entries = parse(Files.readString(paths.serverEvidence)).toOption.get.hcursor.get[Vector[Json]]("entries").toOption.get
    Then("the runtime succeeds with unchanged unrelated arguments and both sessions have closed")
    exit shouldBe 0
    resolver.resolvedClasspaths should not be empty
    invoker.lastArgs.drop(invoker.lastArgs.indexOf("server") + 1) shouldBe _unrelated
    invoker.lastArgs.exists(_.startsWith("--textus.control-center.registration-instance-id=")) shouldBe false
    reporter.starts should have size 1
    reporter.closes shouldBe 1
    reporter.starts.head._2 shouldBe Some("fixture-token")
    val instanceid = reporter.starts.head._1.instanceId
    entries should have size 1
    entries.head.hcursor.get[String]("instanceId").toOption shouldBe Some(instanceid)
    entries.head.hcursor.get[Option[String]]("stoppedAt").toOption.flatten should not be empty
    instanceid
  }

  private def _with_paths[A](run: LauncherPaths => A): A = {
    val parent = Files.createDirectories(Path.of("target", "supervisor-instance-correlation")).toAbsolutePath.normalize
    val directory = Files.createTempDirectory(parent, "case-")
    try {
      val home = Files.createDirectories(directory.resolve("home"))
      val work = Files.createDirectories(directory.resolve("work"))
      run(LauncherPaths(home, work))
    } finally {
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
}

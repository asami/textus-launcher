package textus.launcher

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Aug.  1, 2026
 * @version Aug.  1, 2026
 * @author  ASAMI, Tomoharu
 */
object TextusLauncherTransportSpec {
  def main(args: Array[String]): Unit = {
    val spec = new TextusLauncherTransportSpec
    spec.verifyTransportRuntimeArgumentsWithoutInterpretation()
    println("TextusLauncherTransportSpec: OK")
  }
}

final class TextusLauncherTransportSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  private def _metadata(example: String, rules: String) =
    afterWord(s"in spec:phase-53-cs05-launcher-transport, example:$example, rules:$rules, phase:53, slice:CS-05J")

  "Textus launcher runtime transport" should {
    "E1 retain runtime-owned Web and fixed-user-shaped strings across packaged artifact selection and passthrough" must _metadata("E1", "CS05J-R1,CS05J-R2") {
      "when a packaged CAR server command crosses the Textus wrapper" in {
        Given("Spec: phase-53-cs05-launcher-transport; Rules: CS05J-R1, CS05J-R2; Example: E1; a packaged CAR and runtime-owned arguments separated by passthrough")
        val fixture = _prepare_fixture()
        try {
          When("the wrapper resolves the artifact and delegates to CNCF")
          val results = fixture.run()

          Then("repository/artifact selection is complete and every runtime-owned string retains its exact spelling and order")
          _assert_transport(results)
        } finally {
          fixture.close()
        }
      }
    }
  }

  def verifyTransportRuntimeArgumentsWithoutInterpretation(): Unit = {
    val fixture = _prepare_fixture()
    try _assert_transport(fixture.run())
    finally fixture.close()
  }

  private def _prepare_fixture(): Fixture = {
    val root = Files.createTempDirectory("textus-launcher-transport")
    val paths = LauncherPaths(home = root.resolve("home"), cwd = root.resolve("cwd"))
    val repository = paths.cwd.resolve("repository/car")
    _write(repository.resolve("textus-blog/0.1.0/textus-blog-0.1.0.car"), "")
    _write(
      paths.cwd.resolve(".textus/config.yaml"),
      s"""runtime:
         |  version: 0.5.0
         |  catalog:
         |    url: ${paths.cwd.resolve("missing-runtime-catalog.yaml")}
         |repositories:
         |  car:
         |    - $repository
         |""".stripMargin
    )
    val invoker = new FakeInvoker
    val resolver = FakeResolver()
    val repositoryargs = Vector(
      s"--repository-dir=$repository",
      s"--repository-dir=${paths.localCarRepository}",
      s"--repository-dir=${paths.cacheCarRepository}",
      "--repository-dir=https://www.simplemodeling.org/repository/car",
      s"--repository-dir=${paths.localSarRepository}",
      s"--repository-dir=${paths.cacheSarRepository}",
      "--repository-dir=https://www.simplemodeling.org/repository/sar"
    )
    new Fixture(root, new TextusLauncher(paths, resolver, invoker), invoker, resolver, repositoryargs)
  }

  private def _assert_transport(results: Results): Unit = {
    results.status shouldBe 0
    results.args shouldBe results.repositoryargs ++ Vector(
      "--textus.component=textus-blog",
      "--textus.component.version=0.1.0",
      "server",
      "--textus.web.application-mode=multi-user",
      "--cncf.web.application-mode=standalone",
      "--textus.user.fixed-id=alice",
      "opaque-runtime-value",
      "--launcher-home",
      "runtime-owned-home"
    )
    results.runtimeclasspaths shouldBe Vector("0.5.0")
  }

  private def _write(path: Path, value: String): Unit = {
    Files.createDirectories(path.getParent)
    Files.writeString(path, value, StandardCharsets.UTF_8)
  }

  private def _delete(path: Path): Unit = {
    if (Files.isDirectory(path)) {
      val stream = Files.list(path)
      try stream.forEach(child => _delete(child))
      finally stream.close()
    }
    Files.deleteIfExists(path)
  }

  private final case class Results(
    status: Int,
    args: Vector[String],
    repositoryargs: Vector[String],
    runtimeclasspaths: Vector[String]
  )

  private final class Fixture(
    private val _root: Path,
    private val _launcher: TextusLauncher,
    private val _invoker: FakeInvoker,
    private val _resolver: FakeResolver,
    private val _repository_args: Vector[String]
  ) {
    def run(): Results = {
      val status = _launcher.run(Vector(
        "textus-blog:0.1.0",
        "server",
        "--textus.web.application-mode=multi-user",
        "--cncf.web.application-mode=standalone",
        "--textus.user.fixed-id=alice",
        "--",
        "opaque-runtime-value",
        "--launcher-home",
        "runtime-owned-home"
      ))
      Results(status, _invoker.lastArgs, _repository_args, _resolver.resolvedClasspaths)
    }

    def close(): Unit = _delete(_root)
  }
}

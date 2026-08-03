package textus.launcher

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Aug.  3, 2026
 * @version Aug.  3, 2026
 * @author  ASAMI, Tomoharu
 */
final class Gcf08BindingEnvelopeTransportSpec
  extends AnyWordSpec
    with Matchers
    with GivenWhenThen {
  private val _e1 = afterWord(
    "in spec:phase-55-gcf08j-launcher-binding-envelope-transport, example:E1, rules:GCF08J-R1,R2,R3, phase:55, slice:GCF-08J"
  )

  "Textus launcher binding-envelope transport" should {
    "E1 forward opaque binding-looking tokens without adopting runtime parameter semantics" must _e1 {
      "when launcher configuration, artifact selection, and command passthrough coexist" in {
        Given("a packaged CAR fixture and runtime-owned opaque binding-looking tokens")
        val fixture = _fixture()
        try {
          When("the Textus launcher resolves its own configuration and delegates the artifact command")
          val result = fixture.run()

          Then("launcher configuration and artifact activation are consumed while runtime tokens stay unchanged")
          result.status shouldBe 0
          result.args shouldBe fixture.repositoryargs ++ Vector(
            "--textus.component=textus-blog",
            "--textus.component.version=0.1.0",
            "server"
          ) ++ fixture.prepassthrough ++ fixture.commanddomain
        } finally {
          fixture.close()
        }
      }
    }
  }

  private def _fixture(): Fixture = {
    val root = Files.createTempDirectory("gcf08j-textus-launcher")
    val paths = LauncherPaths(home = root.resolve("home"), cwd = root.resolve("cwd"))
    val repository = paths.cwd.resolve("repository/car")
    val config = paths.cwd.resolve("transport-launcher.yaml")
    _write(repository.resolve("textus-blog/0.1.0/textus-blog-0.1.0.car"), "")
    _write(
      config,
      s"""runtime:
         |  version: 0.5.0
         |  catalog:
         |    url: ${paths.cwd.resolve("missing-runtime-catalog.yaml")}
         |repositories:
         |  car:
         |    - $repository
         |""".stripMargin
    )
    val repositoryargs = Vector(
      s"--repository-dir=$repository",
      s"--repository-dir=${paths.localCarRepository}",
      s"--repository-dir=${paths.cacheCarRepository}",
      "--repository-dir=https://www.simplemodeling.org/repository/car",
      s"--repository-dir=${paths.localSarRepository}",
      s"--repository-dir=${paths.cacheSarRepository}",
      "--repository-dir=https://www.simplemodeling.org/repository/sar"
    )
    new Fixture(
      root,
      paths,
      config,
      repositoryargs,
      Vector(
        "--textus.binding=@s/platform/default:textus.subsystem.user-mode=standalone=with=equals",
        "--textus.binding=@s/platform/default:textus.subsystem.user-mode=",
        "--textus.binding=not-a-canonical-envelope"
      ),
      Vector("--textus.binding=@s/platform/default:textus.subsystem.user-mode=command-domain")
    )
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

  private final class Fixture(
    private val _root: Path,
    private val _paths: LauncherPaths,
    private val _config: Path,
    val repositoryargs: Vector[String],
    val prepassthrough: Vector[String],
    val commanddomain: Vector[String]
  ) {
    def run(): Result = {
      val invoker = new FakeInvoker
      val launcher = new TextusLauncher(
        _paths,
        FakeResolver(),
        invoker
      )
      val status = launcher.run(
        Vector("--config", _config.toString, "textus-blog:0.1.0", "server") ++
          prepassthrough ++ Vector("--") ++ commanddomain
      )
      Result(status, invoker.lastArgs)
    }

    def close(): Unit = _delete(_root)
  }

  private final case class Result(status: Int, args: Vector[String])
}

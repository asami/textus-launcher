package textus.launcher

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import io.circe.Json
import io.circe.parser.parse
import java.io.{BufferedReader, InputStreamReader}
import java.net.{InetAddress, InetSocketAddress, ServerSocket, Socket, SocketTimeoutException, URI, URLDecoder}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.time.{Duration, Instant}
import java.security.KeyStore
import java.security.cert.{CertificateException, X509Certificate}
import java.util.UUID
import java.util.concurrent.{Callable, ConcurrentLinkedQueue, CountDownLatch, ExecutorService, Executors, ThreadFactory, TimeUnit, TimeoutException}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import javax.net.ssl.{KeyManagerFactory, SSLContext, SSLEngine, SSLServerSocket, SSLSocket, TrustManagerFactory, X509ExtendedTrustManager}
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.jdk.CollectionConverters.*
import scala.util.Try
import scala.util.control.NonFatal

/*
 * @since   Oct. 16, 2026
 * @version Oct. 16, 2026
 */
final class CanonicalServerNotificationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "canonical server notification" should {
    "return a ready session while its HTTP response is withheld" in {
      Given("a coherent ready server and a Control Center withholding its first response")
      _with_properties {
        _ready()
        _with_endpoint(true) { endpoint =>
          _with_caller { caller =>
            When("the startup caller starts registration")
            val started = caller.submit(new Callable[TextusControlCenterRegistrationSession] {
              def call(): TextusControlCenterRegistrationSession =
                TextusControlCenterRegistrationReporter.System.start(_config(endpoint), _report(), Some("wire-token"))
            })
            try {
              Then("the caller receives its session before the response is released")
              val session = started.get(1L, TimeUnit.SECONDS)
              try {
                endpoint.entered.await(2L, TimeUnit.SECONDS) shouldBe true
                endpoint.isBlocked shouldBe true
                val workers = Thread.getAllStackTraces.keySet.asScala.filter(_.getName == "textus-control-center-registration-heartbeat")
                workers should not be empty
                workers.forall(_.isDaemon) shouldBe true
              } finally session.close()
            } finally {
              endpoint.release()
              if (started.isDone && !started.isCancelled) started.get().close()
            }
          }
        }
      }
    }

    "retain one coherent public origin and optional application snapshot" in {
      Given("bound generation A, unrelated legacy generation B, and optional public origins")
      _with_properties {
        _with_endpoint() { endpoint =>
          Vector(("", "/web"), ("https://public.example.test", "/web/portal"), ("https://public.example.test/", "")).foreach { case (publicbase, path) =>
            _ready("http://127.0.0.1:38010", path)
            sys.props.update("textus.server.bound-base-url", "http://127.0.0.1:39000")
            sys.props.update("textus.server.bound-application-path", "/web/legacy")
            val report = _report()
            val expectedbase = if (publicbase.isEmpty) "http://127.0.0.1:38010" else publicbase.stripSuffix("/")
            When("the ready session registers and later readiness changes before close")
            _with_session(_config(endpoint, publicbase), report) { session =>
              _await(endpoint.forInstance(report.instanceId).nonEmpty)
              _ready("http://127.0.0.1:39001", "/web/changed")
              session.close()
              _await(endpoint.forInstance(report.instanceId).exists(_.operation == "deregister-subsystem"))
              Then("register and terminal notification use the same captured origin and selected application path")
              val requests = endpoint.forInstance(report.instanceId)
              requests.map(_.operation) shouldBe Vector("register-subsystem", "deregister-subsystem")
              requests.foreach { request =>
                request.parameters("baseUrl") shouldBe expectedbase
                request.parameters.get("applicationUrl") shouldBe Option.when(path.nonEmpty)(expectedbase + path)
              }
            }
          }
        }
      }
    }

    "ignore incomplete or unsafe readiness even when legacy values and public base exist" in {
      Given("legacy readiness and malformed coherent origins or application paths")
      _with_properties {
        sys.props.update("textus.server.bound-base-url", "http://127.0.0.1:38000")
        sys.props.update("textus.server.bound-application-path", "/web/legacy")
        _with_endpoint() { endpoint =>
          Vector("v1\nhttp://127.0.0.1:38000", "v1\nhttp://user@localhost:38000\n/web", "v1\nhttp://localhost:38000\n/web/system", "v1\nhttp://localhost:38000\n/web/../private", "v1\nhttp://localhost:38000\n/web//portal").foreach { snapshot =>
            sys.props.update("textus.server.bound-snapshot", snapshot)
            When("the reporter checks readiness with a valid configured public origin")
            _with_session(_config(endpoint, "https://public.example.test"), _report()) { _ =>
              Then("no request combines legacy values or publishes unsafe readiness")
              endpoint.entered.await(100L, TimeUnit.MILLISECONDS) shouldBe false
            }
          }
          _ready()
          When("the configured public base itself has an unsafe path")
          _with_session(_config(endpoint, "https://public.example.test/prefix"), _report()) { _ =>
            Then("the invalid public origin remains pending")
            endpoint.entered.await(100L, TimeUnit.MILLISECONDS) shouldBe false
          }
        }
      }
    }

    "keep a closed pending session terminal when readiness appears later" in {
      Given("a session with public configuration but no coherent post-bind readiness")
      _with_properties {
        _with_endpoint() { endpoint =>
          _with_session(_config(endpoint, "https://public.example.test"), _report()) { session =>
            When("close precedes publication of readiness")
            session.close()
            session.close()
            _ready()
            Then("neither registration nor terminal HTTP is emitted")
            endpoint.entered.await(200L, TimeUnit.MILLISECONDS) shouldBe false
            endpoint.requests shouldBe empty
          }
        }
      }
    }

    "close an in-flight accepted register without waiting and send one terminal notification" in {
      Given("an initial register whose response is held on the daemon worker")
      _with_properties {
        _ready()
        _with_endpoint(true) { endpoint =>
          val report = _report()
          _with_session(_config(endpoint), report) { session =>
            endpoint.entered.await(2L, TimeUnit.SECONDS) shouldBe true
            _with_caller { caller =>
              When("close races that request and is repeated before acknowledgment")
              caller.submit(new Callable[Unit] { def call(): Unit = session.close() }).get(1L, TimeUnit.SECONDS)
              session.close()
              _ready("http://127.0.0.1:39000", "/web/later")
              Then("close has returned while the response remains held")
              endpoint.isBlocked shouldBe true
              endpoint.release()
              _await(endpoint.forInstance(report.instanceId).exists(_.operation == "deregister-subsystem"))
              And("the bounded accepted completion emits only one stopped report for its original snapshot")
              val requests = endpoint.forInstance(report.instanceId)
              requests.map(_.operation) shouldBe Vector("register-subsystem", "deregister-subsystem")
              requests.last.parameters("launcherState") shouldBe "stopped"
              requests.forall(_.parameters("baseUrl") == "http://127.0.0.1:38000") shouldBe true
            }
          }
        }
      }
    }

    "preserve acknowledgment after a heartbeat failure and retain the captured URL" in {
      Given("a Control Center that accepts registration but rejects one running heartbeat")
      _with_properties {
        _ready()
        _with_endpoint() { endpoint =>
          endpoint.rejectHeartbeat()
          val report = _report()
          When("the serialized notification worker reaches subsequent heartbeat intervals")
          _with_session(_config(endpoint, interval = Duration.ofMillis(10)), report) { session =>
            _await(endpoint.forInstance(report.instanceId).count(_.operation == "heartbeat-subsystem") >= 2)
            _ready("http://127.0.0.1:39000", "/web/later")
            _await(endpoint.forInstance(report.instanceId).count(_.operation == "heartbeat-subsystem") >= 3)
            session.close()
            _await(endpoint.forInstance(report.instanceId).exists(_.operation == "deregister-subsystem"))
            Then("there is one starting report, continuing running reports, and one stopped report")
            val requests = endpoint.forInstance(report.instanceId)
            requests.count(_.operation == "register-subsystem") shouldBe 1
            requests.count(_.operation == "deregister-subsystem") shouldBe 1
            requests.filter(_.operation == "heartbeat-subsystem").forall(_.parameters("launcherState") == "running") shouldBe true
            requests.forall(_.parameters.get("applicationUrl").contains("http://127.0.0.1:38000/web")) shouldBe true
            endpoint.rejectedHeartbeats shouldBe 1
          }
        }
      }
    }

    "round positive submillisecond timeouts and intervals up to finite requests" in {
      Given("a withheld initial response with positive nanosecond timeout and heartbeat settings")
      _with_properties {
        _ready()
        _with_endpoint(true) { endpoint =>
          val report = _report()
          When("the worker attempts registration with those settings")
          _with_session(_config(endpoint, timeout = Duration.ofNanos(1), interval = Duration.ofNanos(1)), report) { session =>
            endpoint.entered.await(2L, TimeUnit.SECONDS) shouldBe true
            Then("a later register is delivered while the first response is still withheld")
            _await(endpoint.forInstance(report.instanceId).count(_.operation == "register-subsystem") >= 2)
            endpoint.isBlocked shouldBe true
            session.close()
          }
        }
      }
    }


    "expire incomplete response headers despite bytes arriving within the idle timeout" in {
      Given("a raw endpoint dripping an unfinished header every 25 ms with a 150 ms notification budget")
      _with_properties {
        _ready()
        _with_raw_endpoint(true) { endpoint =>
          _with_warning_output { warnings =>
            val config = TextusControlCenterRegistrationConfig(endpoint.endpoint, "WIRE_TOKEN", Duration.ofMillis(150), Duration.ofSeconds(30), "spec-host", "")
            val started = System.nanoTime()
            When("the worker receives continued partial headers on its single connection")
            _with_session(config, _report()) { session =>
              endpoint.entered.await(2L, TimeUnit.SECONDS) shouldBe true
              Then("the elapsed deadline closes that connection while the endpoint is still willing to drip")
              endpoint.finished.await(750L, TimeUnit.MILLISECONDS) shouldBe true
              TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) should be < 800L
              endpoint.dripped should be >= 2
              endpoint.connectionCount shouldBe 1
              endpoint.requestCount shouldBe 1
              _await(warnings.toString(StandardCharsets.UTF_8).contains("register-subsystem request failed"))
              val warning = warnings.toString(StandardCharsets.UTF_8)
              warning should not include "wire-token"
              warning should not include "instanceId"
              warning should not include endpoint.endpoint
              session.close()
            }
          }
        }
      }
    }

    "make one connection after early EOF and retry only at the later heartbeat interval" in {
      Given("a raw endpoint closing after each complete GET without any response and a one-second retry interval")
      _with_properties {
        _ready()
        _with_raw_endpoint(false) { endpoint =>
          _with_warning_output { warnings =>
            val config = TextusControlCenterRegistrationConfig(endpoint.endpoint, "WIRE_TOKEN", Duration.ofMillis(150), Duration.ofSeconds(1), "spec-host", "")
            When("the first registration response ends before its status line")
            _with_session(config, _report()) { session =>
              endpoint.finished.await(2L, TimeUnit.SECONDS) shouldBe true
              _await(warnings.toString(StandardCharsets.UTF_8).contains("register-subsystem request failed"))
              Then("there is exactly one GET and no transparent reconnect or resend")
              endpoint.additionalConnection.await(350L, TimeUnit.MILLISECONDS) shouldBe false
              endpoint.connectionCount shouldBe 1
              endpoint.requestCount shouldBe 1
              And("the configured later interval permits exactly the next registration attempt")
              endpoint.retryReceived.await(2L, TimeUnit.SECONDS) shouldBe true
              endpoint.connectionCount shouldBe 2
              endpoint.requestCount shouldBe 2
              val warning = warnings.toString(StandardCharsets.UTF_8)
              warning should not include "wire-token"
              warning should not include "instanceId"
              warning should not include endpoint.endpoint
              session.close()
            }
          }
        }
      }
    }


    "release expired TLS connections and continue retry and terminal delivery while the first trust task remains held" in {
      Given("coherent readiness, a trusted localhost endpoint, and a first delegated trust check held beyond a 200 ms deadline")
      _with_properties {
        _ready()
        val entered = new CountDownLatch(1)
        val release = new CountDownLatch(1)
        val completed = new CountDownLatch(1)
        _with_tls_endpoint_with_trust(manager => new HeldServerTrust(manager, entered, release, completed)) { endpoint =>
          try {
            _with_warning_output { warnings =>
              val report = _report()
              val config = TextusControlCenterRegistrationConfig(endpoint.endpoint, "WIRE_TOKEN", Duration.ofMillis(200), Duration.ofSeconds(2), "spec-host", "")
              When("the real notification worker begins registration and its first trust task remains blocked")
              _with_session(config, report) { session =>
                entered.await(2L, TimeUnit.SECONDS) shouldBe true
                Then("the expired attempt closes its sole connection before the retry interval without sending credentials or waiting for trust completion")
                endpoint.rejected.await(750L, TimeUnit.MILLISECONDS) shouldBe true
                val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(250L)
                while (endpoint.activeConnectionCount != 0 && System.nanoTime() < deadline) Thread.sleep(2L)
                endpoint.activeConnectionCount shouldBe 0
                endpoint.connectionCount shouldBe 1
                endpoint.requests shouldBe empty
                completed.getCount shouldBe 1L
                _await(warnings.toString(StandardCharsets.UTF_8).contains("register-subsystem request failed"))
                val warning = warnings.toString(StandardCharsets.UTF_8)
                warning should not include "wire-token"
                warning should not include "instanceId"
                warning should not include endpoint.endpoint
                When("the same session reaches its later retry interval with the original trust task still held")
                _await(endpoint.forInstance(report.instanceId).exists(_.operation == "register-subsystem"))
                session.close()
                _await(endpoint.forInstance(report.instanceId).exists(_.operation == "deregister-subsystem"))
                session.close()
                Then("normal subsequent trust checks permit one accepted registration and its sole terminal notification before the first task is released")
                completed.getCount shouldBe 1L
                release.getCount shouldBe 1L
                endpoint.connectionCount shouldBe 3
                val requests = endpoint.forInstance(report.instanceId)
                requests.map(_.operation) shouldBe Vector("register-subsystem", "deregister-subsystem")
                requests.foreach { request =>
                  request.method shouldBe "GET"
                  request.authorization shouldBe "Bearer wire-token"
                  request.parameters shouldBe Map(
                    "protocolVersion" -> "1", "instanceId" -> report.instanceId, "launcherKind" -> "textus",
                    "target" -> report.target, "artifactId" -> report.artifactId.get,
                    "executionMode" -> report.executionMode, "runtimeVersion" -> report.runtimeVersion,
                    "baseUrl" -> "http://127.0.0.1:38000", "applicationUrl" -> "http://127.0.0.1:38000/web",
                    "hostLabel" -> "spec-host", "startedAt" -> report.startedAt.toString,
                    "subsystemName" -> report.subsystemName.get, "subsystemVersion" -> report.subsystemVersion.get,
                    "launcherState" -> (if (request.operation == "register-subsystem") "starting" else "stopped")
                  )
                }
              }
            }
          } finally {
            release.countDown()
            if (entered.getCount == 0L) completed.await(3L, TimeUnit.SECONDS) shouldBe true
          }
        }
      }
    }

    "preserve the HTTPS wire lifecycle for a trusted hostname and reject a hostname mismatch before HTTP" in {
      Given("a real localhost certificate, an isolated fixture trust store, and coherent server readiness")
      _with_properties {
        _ready()
        _with_tls_endpoint { endpoint =>
          val report = _report()
          val config = TextusControlCenterRegistrationConfig(endpoint.endpoint, "WIRE_TOKEN", Duration.ofSeconds(2), Duration.ofSeconds(30), "spec-host", "")
          When("the daemon notifies the matching trusted HTTPS hostname and closes its session")
          _with_session(config, report) { session =>
            _await(endpoint.forInstance(report.instanceId).exists(_.operation == "register-subsystem"))
            session.close()
            _await(endpoint.forInstance(report.instanceId).exists(_.operation == "deregister-subsystem"))
            Then("the encrypted GET, bearer credential, exact query fields, and sole terminal attempt retain their existing contract")
            val requests = endpoint.forInstance(report.instanceId)
            requests.map(_.operation) shouldBe Vector("register-subsystem", "deregister-subsystem")
            requests.foreach { request =>
              request.method shouldBe "GET"
              request.authorization shouldBe "Bearer wire-token"
              request.parameters shouldBe Map(
                "protocolVersion" -> "1", "instanceId" -> report.instanceId, "launcherKind" -> "textus",
                "target" -> report.target, "artifactId" -> report.artifactId.get,
                "executionMode" -> report.executionMode, "runtimeVersion" -> report.runtimeVersion,
                "baseUrl" -> "http://127.0.0.1:38000", "applicationUrl" -> "http://127.0.0.1:38000/web",
                "hostLabel" -> "spec-host", "startedAt" -> report.startedAt.toString,
                "subsystemName" -> report.subsystemName.get, "subsystemVersion" -> report.subsystemVersion.get,
                "launcherState" -> (if (request.operation == "register-subsystem") "starting" else "stopped")
              )
            }
            endpoint.hosts shouldBe Vector.fill(2)(s"localhost:${endpoint.port}")
          }
          _with_warning_output { warnings =>
            When("the same trusted certificate is reached through its nonmatching IP hostname")
            _with_session(config.copy(endpoint = endpoint.mismatchedEndpoint), _report()) { session =>
              Then("HTTPS identification fails before any credential-bearing GET reaches the endpoint")
              endpoint.rejected.await(3L, TimeUnit.SECONDS) shouldBe true
              _await(warnings.toString(StandardCharsets.UTF_8).contains("register-subsystem request failed"))
              endpoint.requests.map(_.operation) shouldBe Vector("register-subsystem", "deregister-subsystem")
              endpoint.connectionCount shouldBe 3
              val warning = warnings.toString(StandardCharsets.UTF_8)
              warning should not include "wire-token"
              warning should not include endpoint.mismatchedEndpoint
              session.close()
            }
          }
        }
      }
    }

    "complete the terminal attempt before canonical exit eligibility while an acknowledged heartbeat is withheld" in {
      Given("an acknowledged registration, a withheld heartbeat response, and a canonical server returning exit 27")
      _with_properties {
        _ready()
        _with_paths { paths =>
          _with_endpoint(blockheartbeat = true) { endpoint =>
            val invoker = new CncfInvoker {
              override def invoke(classpath: Vector[Path], args: Vector[String]): Int = {
                endpoint.heartbeatEntered.await(2L, TimeUnit.SECONDS) shouldBe true
                27
              }
            }
            val launcher = _launcher(paths, endpoint, invoker, interval = "20ms")
            _with_caller { caller =>
              When("the acknowledged server finishes while its running report is still pending")
              val result = caller.submit(new Callable[Int] { def call(): Int = launcher.run(_command) })
              try {
                endpoint.heartbeatEntered.await(2L, TimeUnit.SECONDS) shouldBe true
                _await(Files.isRegularFile(paths.serverEvidence) && _entries(paths).exists(_.hcursor.get[Option[String]]("stoppedAt").toOption.flatten.nonEmpty))
                Then("durable evidence is stopped but the actual invocation return remains pending within its finite cleanup bound")
                endpoint.isBlocked shouldBe true
                intercept[TimeoutException] { result.get(100L, TimeUnit.MILLISECONDS) }
                endpoint.requests.map(_.operation) shouldBe Vector("register-subsystem", "heartbeat-subsystem")
                When("the controlled endpoint releases the outstanding heartbeat")
                endpoint.release()
                val exit = result.get(3L, TimeUnit.SECONDS)
                Then("the sole terminal attempt has already reached the endpoint when the original exit becomes eligible")
                exit shouldBe 27
                endpoint.requests.map(_.operation) shouldBe Vector("register-subsystem", "heartbeat-subsystem", "deregister-subsystem")
                endpoint.requests.last.parameters("launcherState") shouldBe "stopped"
                _entries(paths) should have size 1
                _entries(paths).forall(_.hcursor.get[Option[String]]("stoppedAt").toOption.flatten.nonEmpty) shouldBe true
                _no_authority(paths)
              } finally endpoint.release()
            }
          }
        }
      }
    }

    "run the canonical server and persist stopped evidence while notification is held" in {
      Given("a canonical target, shared evidence store, and a withheld Control Center response")
      _with_properties {
        _ready()
        _with_paths { paths =>
          _with_endpoint(true) { endpoint =>
            val observed = new AtomicBoolean(false)
            val invoker = new CncfInvoker {
              override def invoke(classpath: Vector[Path], args: Vector[String]): Int = {
                observed.set(_entries(paths).exists(_.hcursor.get[Option[String]]("stoppedAt").toOption.contains(None)))
                endpoint.entered.await(2L, TimeUnit.SECONDS) shouldBe true
                27
              }
            }
            val launcher = _launcher(paths, endpoint, invoker)
            _with_caller { caller =>
              When("the canonical invocation finishes before the notification response")
              val result = caller.submit(new Callable[Int] { def call(): Int = launcher.run(_command) })
              try {
                Then("the original exit and durable stopped evidence are independent from notification")
                result.get(3L, TimeUnit.SECONDS) shouldBe 27
                observed.get shouldBe true
                _entries(paths) should have size 1
                _entries(paths).forall(_.hcursor.get[Option[String]]("stoppedAt").toOption.flatten.nonEmpty) shouldBe true
                endpoint.isBlocked shouldBe true
                _no_authority(paths)
              } finally endpoint.release()
              _await(endpoint.requests.exists(_.operation == "deregister-subsystem"))
            }
          }
        }
      }
    }

    "preserve stopped evidence and the original exit or exception when registration close throws" in {
      Given("a canonical invocation with a fallible registration close")
      _with_properties {
        _with_endpoint() { endpoint =>
          Vector(false, true).foreach { throwing =>
            _with_paths { paths =>
              val original = new IllegalArgumentException("original server failure")
              val stoppedbeforeclose = new AtomicBoolean(false)
              val reporter = new TextusControlCenterRegistrationReporter {
                def start(config: TextusControlCenterRegistrationConfig, report: TextusControlCenterRegistrationReport, token: Option[String]): TextusControlCenterRegistrationSession =
                  new TextusControlCenterRegistrationSession {
                    def close(): Unit = {
                      stoppedbeforeclose.set(_entries(paths).forall(_.hcursor.get[Option[String]]("stoppedAt").toOption.flatten.nonEmpty))
                      throw new IllegalStateException("registration close unavailable")
                    }
                  }
              }
              val invoker = new CncfInvoker {
                override def invoke(classpath: Vector[Path], args: Vector[String]): Int = {
                  if (throwing) throw original
                  27
                }
              }
              val launcher = _launcher(paths, endpoint, invoker, reporter)
              When("the server returns or throws and cleanup closes its sessions")
              val outcome = Try(launcher.run(_command))
              Then("durable evidence was stopped first and close did not replace the invocation outcome")
              stoppedbeforeclose.get shouldBe true
              _entries(paths) should have size 1
              if (throwing) outcome.failed.get shouldBe original else outcome.get shouldBe 27
              _no_authority(paths)
              endpoint.requests shouldBe empty
            }
          }
        }
      }
    }

    "correlate at least fifty generated opaque identities through the real wire lifecycle" in {
      Given("generated safe identities, targets, and public origins with an actual loopback endpoint")
      _with_properties {
        _with_endpoint() { endpoint =>
          val opaque = Gen.listOfN(24, Gen.alphaNumChar).map(_.mkString)
          val cases = for {
            identity <- opaque
            target <- opaque
            host <- opaque
            port <- Gen.choose(1024, 65535)
          } yield (s"instance-$identity", s"target-$target", s"https://${host.toLowerCase}.example.test:$port")
          When("ScalaCheck executes fifty serialized register, heartbeat, and terminal wire sessions")
          val result = Test.check(Test.Parameters.default.withMinSuccessfulTests(50).withWorkers(1), Prop.forAll(cases) { case (identity, target, origin) =>
            Given("one generated opaque invocation and a coherent application route")
            _ready("http://127.0.0.1:38000", "/web/portal")
            val report = _report(identity, target)
            When("its real registration is acknowledged, heartbeats, and closes")
            _with_session(_config(endpoint, origin, interval = Duration.ofMillis(2)), report) { session =>
              _await(endpoint.forInstance(identity).exists(_.operation == "heartbeat-subsystem"))
              session.close()
              _await(endpoint.forInstance(identity).exists(_.operation == "deregister-subsystem"))
              Then("all actual wire reports preserve its generated identity and public projection")
              val requests = endpoint.forInstance(identity)
              requests.count(_.operation == "register-subsystem") shouldBe 1
              requests.count(_.operation == "deregister-subsystem") shouldBe 1
              requests.foreach { request =>
                request.method shouldBe "GET"
                request.authorization shouldBe "Bearer wire-token"
                request.parameters("protocolVersion") shouldBe "1"
                request.parameters("instanceId") shouldBe identity
                request.parameters("target") shouldBe target
                request.parameters("artifactId") shouldBe target
                request.parameters("launcherKind") shouldBe "textus"
                request.parameters("baseUrl") shouldBe origin
                request.parameters("applicationUrl") shouldBe origin + "/web/portal"
                request.parameters("startedAt") shouldBe report.startedAt.toString
                request.parameters.contains("token") shouldBe false
              }
            }
            true
          })
          Then("every generated identity retains its exact target, origin, credential, and correlation")
          result.passed shouldBe true
          result.succeeded should be >= 50
        }
      }
    }

  }

  private final case class WireRequest(operation: String, parameters: Map[String, String], authorization: String, method: String)

  private final class Endpoint(blockregister: Boolean, blockheartbeat: Boolean) {
    private val _requests = new ConcurrentLinkedQueue[WireRequest]()
    private val _release = new CountDownLatch(if (blockregister || blockheartbeat) 1 else 0)
    private val _block_next = new AtomicBoolean(blockregister)
    private val _block_heartbeat = new AtomicBoolean(blockheartbeat)
    private val _reject_next = new AtomicBoolean(false)
    private val _rejections = new AtomicInteger(0)
    private val _pool = Executors.newCachedThreadPool(_thread_factory)
    private val _server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0)
    val entered = new CountDownLatch(1)
    val heartbeatEntered = new CountDownLatch(1)
    _server.setExecutor(_pool)
    _server.createContext("/", new HttpHandler {
      def handle(exchange: HttpExchange): Unit = {
        try {
          val operation = exchange.getRequestURI.getPath.split("/").last
          val parameters = Option(exchange.getRequestURI.getRawQuery).toVector.flatMap(_.split("&")).map { pair =>
            val parts = pair.split("=", 2)
            URLDecoder.decode(parts(0), StandardCharsets.UTF_8) -> URLDecoder.decode(parts.lift(1).getOrElse(""), StandardCharsets.UTF_8)
          }.toMap
          _requests.add(WireRequest(operation, parameters, exchange.getRequestHeaders.getFirst("Authorization"), exchange.getRequestMethod))
          entered.countDown()
          if (operation == "heartbeat-subsystem") heartbeatEntered.countDown()
          if (operation == "register-subsystem" && _block_next.compareAndSet(true, false)) {
            _release.await(5L, TimeUnit.SECONDS)
          }
          if (operation == "heartbeat-subsystem" && _block_heartbeat.compareAndSet(true, false)) {
            _release.await(5L, TimeUnit.SECONDS)
          }
          val rejected = operation == "heartbeat-subsystem" && _reject_next.compareAndSet(true, false)
          if (rejected) _rejections.incrementAndGet()
          exchange.sendResponseHeaders(if (rejected) 503 else 204, -1L)
        } finally exchange.close()
      }
    })
    _server.start()
    def endpoint: String = s"http://127.0.0.1:${_server.getAddress.getPort}/inventory"
    def port: Int = _server.getAddress.getPort
    def requests: Vector[WireRequest] = _requests.iterator.asScala.toVector
    def forInstance(instanceId: String): Vector[WireRequest] = requests.filter(_.parameters.get("instanceId").contains(instanceId))
    def isBlocked: Boolean = _release.getCount > 0
    def release(): Unit = _release.countDown()
    def rejectHeartbeat(): Unit = _reject_next.set(true)
    def rejectedHeartbeats: Int = _rejections.get
    def close(): Unit = {
      release()
      _server.stop(0)
      _pool.shutdownNow()
      _pool.awaitTermination(2L, TimeUnit.SECONDS)
      ()
    }
  }



  private final class TlsEndpoint(servercontext: SSLContext, val trustedContext: SSLContext) {
    private val _closed = new AtomicBoolean(false)
    private val _connections = new AtomicInteger(0)
    private val _clients = new ConcurrentLinkedQueue[SSLSocket]()
    private val _requests = new ConcurrentLinkedQueue[WireRequest]()
    private val _hosts = new ConcurrentLinkedQueue[String]()
    private val _pool = Executors.newCachedThreadPool(_thread_factory)
    private val _server = servercontext.getServerSocketFactory.createServerSocket(0, 8, InetAddress.getByName("127.0.0.1")).asInstanceOf[SSLServerSocket]
    val rejected = new CountDownLatch(1)
    _server.setSoTimeout(100)
    _pool.submit(new Runnable {
      def run(): Unit = {
        while (!_closed.get) {
          try {
            val client = _server.accept().asInstanceOf[SSLSocket]
            _clients.add(client)
            _connections.incrementAndGet()
            _pool.submit(new Runnable { def run(): Unit = _serve(client) })
          } catch {
            case _: SocketTimeoutException => ()
            case NonFatal(_) => ()
          }
        }
      }
    })

    def port: Int = _server.getLocalPort
    def endpoint: String = s"https://localhost:$port/inventory"
    def mismatchedEndpoint: String = s"https://127.0.0.1:$port/inventory"
    def connectionCount: Int = _connections.get
    def activeConnectionCount: Int = _clients.size()
    def requests: Vector[WireRequest] = _requests.iterator.asScala.toVector
    def hosts: Vector[String] = _hosts.iterator.asScala.toVector
    def forInstance(instanceId: String): Vector[WireRequest] = requests.filter(_.parameters.get("instanceId").contains(instanceId))

    private def _serve(client: SSLSocket): Unit = {
      try {
        client.setSoTimeout(3000)
        client.startHandshake()
        val reader = new BufferedReader(new InputStreamReader(client.getInputStream, StandardCharsets.ISO_8859_1))
        val requestline = Option(reader.readLine()).getOrElse(throw new java.io.IOException("TLS fixture request missing"))
        val requestparts = requestline.split(" ", 3)
        val uri = URI.create(requestparts(1))
        val parameters = Option(uri.getRawQuery).toVector.flatMap(_.split("&")).map { pair =>
          val parts = pair.split("=", 2)
          URLDecoder.decode(parts(0), StandardCharsets.UTF_8) -> URLDecoder.decode(parts.lift(1).getOrElse(""), StandardCharsets.UTF_8)
        }.toMap
        var authorization = ""
        var host = ""
        var line = reader.readLine()
        while (line != null && line.nonEmpty) {
          val colon = line.indexOf(':')
          if (colon > 0) {
            val name = line.take(colon)
            val value = line.drop(colon + 1).trim
            if (name.equalsIgnoreCase("Authorization")) authorization = value
            if (name.equalsIgnoreCase("Host")) host = value
          }
          line = reader.readLine()
        }
        if (line == null) throw new java.io.IOException("TLS fixture headers incomplete")
        _hosts.add(host)
        _requests.add(WireRequest(uri.getPath.split("/").last, parameters, authorization, requestparts(0)))
        val output = client.getOutputStream
        output.write("HTTP/1.1 204 No Content\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
        output.flush()
      } catch {
        case NonFatal(_) => rejected.countDown()
      } finally {
        try client.close() finally _clients.remove(client)
      }
    }

    def close(): Unit = {
      _closed.set(true)
      _server.close()
      _clients.iterator.asScala.foreach(_.close())
      _pool.shutdownNow()
      _pool.awaitTermination(2L, TimeUnit.SECONDS)
      ()
    }
  }

  private final class HeldServerTrust(
    delegate: X509ExtendedTrustManager,
    entered: CountDownLatch,
    release: CountDownLatch,
    completed: CountDownLatch
  ) extends X509ExtendedTrustManager {
    private val _first = new AtomicBoolean(true)

    override def getAcceptedIssuers(): Array[X509Certificate] = delegate.getAcceptedIssuers
    override def checkClientTrusted(chain: Array[X509Certificate], authType: String): Unit =
      delegate.checkClientTrusted(chain, authType)
    override def checkServerTrusted(chain: Array[X509Certificate], authType: String): Unit =
      delegate.checkServerTrusted(chain, authType)
    override def checkClientTrusted(chain: Array[X509Certificate], authType: String, socket: Socket): Unit =
      delegate.checkClientTrusted(chain, authType, socket)
    override def checkServerTrusted(chain: Array[X509Certificate], authType: String, socket: Socket): Unit =
      delegate.checkServerTrusted(chain, authType, socket)
    override def checkClientTrusted(chain: Array[X509Certificate], authType: String, engine: SSLEngine): Unit =
      delegate.checkClientTrusted(chain, authType, engine)
    override def checkServerTrusted(chain: Array[X509Certificate], authType: String, engine: SSLEngine): Unit = {
      if (_first.compareAndSet(true, false)) {
        var interrupted = false
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15L)
        entered.countDown()
        try {
          var released = false
          while (!released && System.nanoTime() < deadline) {
            try released = release.await(math.max(1L, deadline - System.nanoTime()), TimeUnit.NANOSECONDS)
            catch { case _: InterruptedException => interrupted = true }
          }
          if (!released) throw new CertificateException("TLS fixture trust hold timed out")
          delegate.checkServerTrusted(chain, authType, engine)
        } finally {
          completed.countDown()
          if (interrupted) Thread.currentThread().interrupt()
        }
      } else delegate.checkServerTrusted(chain, authType, engine)
    }
  }

  private def _with_tls_endpoint(f: TlsEndpoint => Unit): Unit =
    _with_tls_endpoint_with_trust(manager => manager)(f)

  private def _with_tls_endpoint_with_trust(transform: X509ExtendedTrustManager => X509ExtendedTrustManager)(f: TlsEndpoint => Unit): Unit = {
    val parent = Files.createDirectories(Path.of("target", "canonical-notification-fixtures")).toAbsolutePath.normalize
    val root = Files.createTempDirectory(parent, "tls-")
    val previous = SSLContext.getDefault
    var fixture = Option.empty[TlsEndpoint]
    try {
      val keystore = root.resolve("localhost.p12")
      val password = "notification-fixture-password"
      val keytool = Path.of(System.getProperty("java.home"), "bin", "keytool")
      val process = new ProcessBuilder(
        keytool.toString, "-genkeypair", "-alias", "notification-fixture",
        "-keyalg", "RSA", "-keysize", "2048", "-storetype", "PKCS12",
        "-keystore", keystore.toString, "-storepass", password, "-keypass", password,
        "-dname", "CN=localhost", "-ext", "SAN=dns:localhost", "-validity", "2", "-noprompt"
      ).directory(root.toFile).redirectErrorStream(true).redirectOutput(root.resolve("keytool.log").toFile).start()
      try {
        if (!process.waitFor(15L, TimeUnit.SECONDS))
          throw new java.io.IOException("TLS fixture certificate generation timed out")
        if (process.exitValue() != 0)
          throw new java.io.IOException("TLS fixture certificate generation failed")
      } finally {
        if (process.isAlive) {
          process.destroyForcibly()
          process.waitFor(2L, TimeUnit.SECONDS)
        }
      }
      val store = KeyStore.getInstance("PKCS12")
      val input = Files.newInputStream(keystore)
      try store.load(input, password.toCharArray) finally input.close()
      val truststore = KeyStore.getInstance("PKCS12")
      truststore.load(null, null)
      truststore.setCertificateEntry("notification-fixture", store.getCertificate("notification-fixture"))
      val keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
      keys.init(store, password.toCharArray)
      val trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
      trust.init(truststore)
      val servercontext = SSLContext.getInstance("TLS")
      servercontext.init(keys.getKeyManagers, trust.getTrustManagers, null)
      val trustedcontext = SSLContext.getInstance("TLS")
      val managers = trust.getTrustManagers.map {
        case manager: X509ExtendedTrustManager => transform(manager)
        case manager => manager
      }
      trustedcontext.init(null, managers, null)
      val endpoint = new TlsEndpoint(servercontext, trustedcontext)
      fixture = Some(endpoint)
      SSLContext.setDefault(endpoint.trustedContext)
      f(endpoint)
    } finally {
      try SSLContext.setDefault(previous) finally {
        try fixture.foreach(_.close()) finally {
          val files = Files.walk(root)
          try files.iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.deleteIfExists(_))
          finally files.close()
        }
      }
    }
  }

  private final class RawEndpoint(dripheaders: Boolean) {
    private val _closed = new AtomicBoolean(false)
    private val _connections = new AtomicInteger(0)
    private val _requests = new AtomicInteger(0)
    private val _dripped = new AtomicInteger(0)
    private val _clients = new ConcurrentLinkedQueue[Socket]()
    private val _pool = Executors.newCachedThreadPool(_thread_factory)
    private val _server = new ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))
    val entered = new CountDownLatch(1)
    val finished = new CountDownLatch(1)
    val additionalConnection = new CountDownLatch(1)
    val retryReceived = new CountDownLatch(1)
    _server.setSoTimeout(100)
    _pool.submit(new Runnable {
      def run(): Unit = {
        while (!_closed.get) {
          try {
            val client = _server.accept()
            _clients.add(client)
            if (_connections.incrementAndGet() > 1) additionalConnection.countDown()
            _pool.submit(new Runnable { def run(): Unit = _serve(client) })
          } catch {
            case _: SocketTimeoutException => ()
            case NonFatal(_) => ()
          }
        }
      }
    })

    def endpoint: String = s"http://127.0.0.1:${_server.getLocalPort}/inventory"
    def connectionCount: Int = _connections.get
    def requestCount: Int = _requests.get
    def dripped: Int = _dripped.get

    private def _serve(client: Socket): Unit = {
      try {
        client.setSoTimeout(2000)
        val reader = new BufferedReader(new InputStreamReader(client.getInputStream, StandardCharsets.US_ASCII))
        var line = reader.readLine()
        while (line != null && line.nonEmpty) line = reader.readLine()
        if (line != null) {
          if (_requests.incrementAndGet() > 1) retryReceived.countDown()
          entered.countDown()
          if (dripheaders) {
            val output = client.getOutputStream
            output.write("HTTP/1.1 204 No Content\r\nX-Incomplete: ".getBytes(StandardCharsets.US_ASCII))
            output.flush()
            while (!_closed.get) {
              output.write('x'.toInt)
              output.flush()
              _dripped.incrementAndGet()
              Thread.sleep(25L)
            }
          }
        }
      } catch {
        case _: InterruptedException => Thread.currentThread().interrupt()
        case NonFatal(_) => ()
      } finally {
        try client.close() finally {
          _clients.remove(client)
          finished.countDown()
        }
      }
    }

    def close(): Unit = {
      _closed.set(true)
      _server.close()
      _clients.iterator.asScala.foreach(_.close())
      _pool.shutdownNow()
      _pool.awaitTermination(2L, TimeUnit.SECONDS)
      ()
    }
  }

  private def _with_raw_endpoint(dripheaders: Boolean)(f: RawEndpoint => Unit): Unit = {
    val endpoint = new RawEndpoint(dripheaders)
    try f(endpoint) finally endpoint.close()
  }

  private def _with_warning_output(f: java.io.ByteArrayOutputStream => Unit): Unit = {
    val output = new java.io.ByteArrayOutputStream()
    val stream = new java.io.PrintStream(output)
    try Console.withErr(stream) { f(output) } finally stream.close()
  }

  private def _thread_factory: ThreadFactory = new ThreadFactory {
    def newThread(runnable: Runnable): Thread = {
      val thread = new Thread(runnable, "notification-spec-worker")
      thread.setDaemon(true)
      thread
    }
  }

  private def _with_endpoint(blockregister: Boolean = false, blockheartbeat: Boolean = false)(f: Endpoint => Unit): Unit = {
    val endpoint = new Endpoint(blockregister, blockheartbeat)
    try f(endpoint) finally endpoint.close()
  }

  private def _with_caller(f: ExecutorService => Unit): Unit = {
    val caller = Executors.newSingleThreadExecutor(_thread_factory)
    try f(caller) finally {
      caller.shutdownNow()
      caller.awaitTermination(2L, TimeUnit.SECONDS)
    }
  }

  private def _with_properties(f: => Unit): Unit = {
    val keys = Vector("textus.server.bound-snapshot", "textus.server.bound-base-url", "textus.server.bound-application-path")
    val previous = keys.map(key => key -> sys.props.get(key))
    keys.foreach(sys.props.remove)
    try f finally previous.foreach {
      case (key, Some(value)) => sys.props.update(key, value)
      case (key, None) => sys.props.remove(key)
    }
  }

  private def _ready(baseurl: String = "http://127.0.0.1:38000", path: String = "/web"): Unit =
    sys.props.update("textus.server.bound-snapshot", s"v1\n$baseurl\n$path")

  private def _await(condition: => Boolean): Unit = {
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
    while (!condition && System.nanoTime() < deadline) Thread.sleep(2L)
    condition shouldBe true
  }

  private def _config(endpoint: Endpoint, baseurl: String = "", timeout: Duration = Duration.ofSeconds(2), interval: Duration = Duration.ofSeconds(30)): TextusControlCenterRegistrationConfig =
    TextusControlCenterRegistrationConfig(endpoint.endpoint, "WIRE_TOKEN", timeout, interval, "spec-host", baseurl)

  private def _report(instanceid: String = UUID.randomUUID().toString, target: String = "notification-component"): TextusControlCenterRegistrationReport =
    TextusControlCenterRegistrationReport(instanceid, target, Some(target), "artifact", None, Some(target), Some("1.0.0"), "0.4.12", Instant.parse("2026-10-16T00:00:00Z"))

  private def _with_session(config: TextusControlCenterRegistrationConfig, report: TextusControlCenterRegistrationReport)(f: TextusControlCenterRegistrationSession => Unit): Unit = {
    val session = TextusControlCenterRegistrationReporter.System.start(config, report, Some("wire-token"))
    try f(session) finally session.close()
  }

  private def _with_paths(f: LauncherPaths => Unit): Unit = {
    val parent = Files.createDirectories(Path.of("target", "canonical-notification-fixtures"))
    val root = Files.createTempDirectory(parent, "session-")
    val home = Files.createDirectories(root.resolve("home")).toAbsolutePath.normalize
    val work = Files.createDirectories(root.resolve("work")).toAbsolutePath.normalize
    f(LauncherPaths(home, work))
  }

  private def _write(path: Path, value: String): Unit = {
    Files.createDirectories(path.getParent)
    Files.writeString(path, value)
    ()
  }

  private def _entries(paths: LauncherPaths): Vector[Json] =
    parse(Files.readString(paths.serverEvidence)).toOption.get.hcursor.get[Vector[Json]]("entries").toOption.get

  private def _no_authority(paths: LauncherPaths): Unit = {
    Vector("supervisor.yaml", "supervisor-state.json", "supervisor.log").foreach { name =>
      Files.exists(paths.cncfHome.resolve("launcher").resolve(name)) shouldBe false
    }
  }

  private def _launcher(paths: LauncherPaths, endpoint: Endpoint, invoker: CncfInvoker, reporter: TextusControlCenterRegistrationReporter = TextusControlCenterRegistrationReporter.System, interval: String = "30s"): TextusLauncher = {
    val repository = paths.cwd.resolve("repo/car")
    _write(repository.resolve("notification-component/1.0.0/notification-component-1.0.0.car"), "")
    _write(paths.cwd.resolve(".textus/config.yaml"), s"""runtime:
      |  version: 0.4.12
      |repositories:
      |  car:
      |    - $repository
      |textus-control-center:
      |  registration:
      |    enabled: true
      |    endpoint: ${endpoint.endpoint}
      |    token-env: WIRE_TOKEN
      |    timeout: 2s
      |    heartbeat-interval: $interval
      |    host-label: spec-host
      |""".stripMargin)
    new TextusLauncher(paths, FakeResolver(), invoker, environment = Map("WIRE_TOKEN" -> "wire-token"), registrationreporter = reporter)
  }

  private val _command = Vector("notification-component", "server")
}

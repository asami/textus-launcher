package textus.launcher

import java.io.{ByteArrayOutputStream, EOFException, IOException}
import java.net.{InetAddress, InetSocketAddress, SocketTimeoutException, URI, URLEncoder}
import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, Selector, SocketChannel}
import java.nio.charset.StandardCharsets
import java.time.{Duration, Instant}
import java.util.concurrent.{Callable, CountDownLatch, Executors, ForkJoinPool, Future, ScheduledFuture, TimeUnit}
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.{SNIHostName, SSLContext, SSLEngineResult}
import scala.util.control.NonFatal

/*
 * @since   Jul. 18, 2026
 *  version Jul. 24, 2026
 *  version Aug. 5, 2026
 *  version Aug. 28, 2026
 * @version Oct. 17, 2026
 * @author  ASAMI, Tomoharu
 */
final case class TextusControlCenterRegistrationConfig(
  endpoint: String,
  tokenEnv: String,
  timeout: Duration,
  heartbeatInterval: Duration,
  hostLabel: String,
  baseUrl: String
)

object TextusControlCenterRegistrationConfig {
  def enabled(values: Map[String, Vector[String]]): Option[Boolean] =
    _first(values, "textus-control-center.registration.enabled", "textus-admin.registration.enabled", "textus.admin.registration.enabled").map(_boolean)

  def fromParsed(values: Map[String, Vector[String]]): Option[TextusControlCenterRegistrationConfig] = {
    def _first_(keys: String*): Option[String] =
      _first(values, keys*)

    if (!enabled(values).contains(true)) {
      None
    } else {
      Some(TextusControlCenterRegistrationConfig(
        _first_("textus-control-center.registration.endpoint", "textus-admin.registration.endpoint", "textus.admin.registration.endpoint").getOrElse(throw TextusException("textus-control-center.registration.endpoint is required when registration is enabled")),
        _first_("textus-control-center.registration.token-env", "textus-control-center.registration.tokenEnv", "textus-admin.registration.token-env", "textus-admin.registration.tokenEnv", "textus.admin.registration.token-env", "textus.admin.registration.tokenEnv").getOrElse(throw TextusException("textus-control-center.registration.token-env is required when registration is enabled")),
        _duration(_first_("textus-control-center.registration.timeout", "textus-admin.registration.timeout", "textus.admin.registration.timeout").getOrElse("2s"), "textus-control-center.registration.timeout"),
        _duration(_first_("textus-control-center.registration.heartbeat-interval", "textus-control-center.registration.heartbeatInterval", "textus-admin.registration.heartbeat-interval", "textus-admin.registration.heartbeatInterval", "textus.admin.registration.heartbeat-interval", "textus.admin.registration.heartbeatInterval").getOrElse("30s"), "textus-control-center.registration.heartbeat-interval"),
        _first_("textus-control-center.registration.host-label", "textus-control-center.registration.hostLabel", "textus-admin.registration.host-label", "textus-admin.registration.hostLabel", "textus.admin.registration.host-label", "textus.admin.registration.hostLabel").getOrElse(throw TextusException("textus-control-center.registration.host-label is required when registration is enabled")),
        _first_("textus-control-center.registration.base-url", "textus-control-center.registration.baseUrl", "textus-admin.registration.base-url", "textus-admin.registration.baseUrl", "textus.admin.registration.base-url", "textus.admin.registration.baseUrl").getOrElse("")
      ))
    }
  }

  private def _first(values: Map[String, Vector[String]], keys: String*): Option[String] =
    keys.toVector.flatMap(key => values.getOrElse(key, Vector.empty)).headOption.map(_.trim).filter(_.nonEmpty)

  private def _boolean(value: String): Boolean =
    Set("true", "yes", "on", "1").contains(value.toLowerCase)

  private def _duration(value: String, name: String): Duration = {
    val normalized = value.trim
    val duration =
      if (normalized.matches("[0-9]+ms")) Duration.ofMillis(normalized.stripSuffix("ms").toLong)
      else if (normalized.matches("[0-9]+s")) Duration.ofSeconds(normalized.stripSuffix("s").toLong)
      else if (normalized.matches("[0-9]+m")) Duration.ofMinutes(normalized.stripSuffix("m").toLong)
      else scala.util.Try(Duration.parse(normalized)).getOrElse(throw TextusException(s"$name must be a positive duration"))
    if (duration.isZero || duration.isNegative)
      throw TextusException(s"$name must be a positive duration")
    duration
  }
}

final case class TextusControlCenterRegistrationReport(
  instanceId: String,
  target: String,
  artifactId: Option[String],
  executionMode: String,
  developmentDirectory: Option[String],
  subsystemName: Option[String],
  subsystemVersion: Option[String],
  runtimeVersion: String,
  startedAt: Instant
)

trait TextusControlCenterRegistrationSession {
  def close(): Unit
}

trait TextusControlCenterRegistrationReporter {
  def start(
    config: TextusControlCenterRegistrationConfig,
    report: TextusControlCenterRegistrationReport,
    token: Option[String]
  ): TextusControlCenterRegistrationSession
}

object TextusControlCenterRegistrationReporter {
  val System: TextusControlCenterRegistrationReporter = new SystemTextusControlCenterRegistrationReporter
}

private final class SystemTextusControlCenterRegistrationReporter extends TextusControlCenterRegistrationReporter {
  def start(
    config: TextusControlCenterRegistrationConfig,
    report: TextusControlCenterRegistrationReport,
    token: Option[String]
  ): TextusControlCenterRegistrationSession = {
    token match {
      case None =>
        _warning("credential is unavailable")
        TextusControlCenterRegistrationSession.noop
      case Some(value) if value.trim.nonEmpty =>
        _start_pending(config, report, value)
      case Some(_) =>
        _warning("credential is unavailable")
        TextusControlCenterRegistrationSession.noop
    }
  }

  private def _start_pending(
    config: TextusControlCenterRegistrationConfig,
    report: TextusControlCenterRegistrationReport,
    token: String
  ): TextusControlCenterRegistrationSession = {
    val executor = Executors.newSingleThreadScheduledExecutor(_daemon_thread_factory)
    val closed = new AtomicBoolean(false)
    val acknowledged = new AtomicBoolean(false)
    val terminal = new AtomicBoolean(false)
    val terminaldone = new CountDownLatch(1)
    val gate = new Object
    var snapshot = Option.empty[RegistrationSnapshot]
    var readiness = Option.empty[ScheduledFuture[?]]
    var heartbeat = Option.empty[ScheduledFuture[?]]

    def _terminal_(): Unit = {
      try {
        if (acknowledged.get && terminal.compareAndSet(false, true)) {
          snapshot.foreach(value => _request_best_effort(config, report, token, value, "deregister-subsystem", "stopped"))
        }
      } finally terminaldone.countDown()
    }

    val notifytask = new Runnable {
      def run(): Unit = {
        if (!closed.get) {
          snapshot.foreach { value =>
            if (acknowledged.get) {
              _request_best_effort(config, report, token, value, "heartbeat-subsystem", "running")
            } else if (_request_best_effort(config, report, token, value, "register-subsystem", "starting")) {
              acknowledged.set(true)
              if (!closed.get)
                _request_best_effort(config, report, token, value, "heartbeat-subsystem", "running")
            }
          }
        }
        if (closed.get) _terminal_()
      }
    }
    val readytask = new Runnable {
      def run(): Unit = gate.synchronized {
        if (!closed.get && snapshot.isEmpty) {
          _snapshot(config).foreach { value =>
            snapshot = Some(value)
            readiness.foreach(_.cancel(false))
            val interval = _positive_millis(config.heartbeatInterval, Long.MaxValue)
            heartbeat = Some(executor.scheduleWithFixedDelay(notifytask, 0L, interval, TimeUnit.MILLISECONDS))
          }
        }
      }
    }
    gate.synchronized {
      readiness = Some(executor.scheduleWithFixedDelay(readytask, 0L, 50L, TimeUnit.MILLISECONDS))
    }
    new TextusControlCenterRegistrationSession {
      def close(): Unit = {
        val waitforterminal = gate.synchronized {
          val wasacknowledged = acknowledged.get
          if (closed.compareAndSet(false, true)) {
            readiness.foreach(_.cancel(false))
            heartbeat.foreach(_.cancel(false))
            // The existing daemon owns both an in-flight request and terminal work.
            executor.execute(new Runnable { def run(): Unit = _terminal_() })
            executor.shutdown()
          }
          wasacknowledged
        }
        if (waitforterminal) {
          try terminaldone.await(4L * _positive_millis(config.timeout, 5000L) + 500L, TimeUnit.MILLISECONDS)
          catch { case _: InterruptedException => Thread.currentThread().interrupt() }
        }
      }
    }
  }

  private def _positive_millis(duration: Duration, maximum: Long): Long =
    if (duration.compareTo(Duration.ofMillis(maximum)) >= 0) maximum
    else math.max(1L, duration.toMillis)

  private def _request_best_effort(
    config: TextusControlCenterRegistrationConfig,
    report: TextusControlCenterRegistrationReport,
    token: String,
    snapshot: RegistrationSnapshot,
    operation: String,
    state: String
  ): Boolean =
    try {
      val status = _request(config, report, token, snapshot, operation, state)
      if (status < 200 || status >= 300) {
        _warning(s"$operation returned HTTP $status")
        false
      } else {
        true
      }
    } catch {
      case NonFatal(_) =>
        _warning(s"$operation request failed")
        false
    }

  private def _request(
    config: TextusControlCenterRegistrationConfig,
    report: TextusControlCenterRegistrationReport,
    token: String,
    snapshot: RegistrationSnapshot,
    operation: String,
    state: String
  ): Int = {
    val endpoint = URI.create(_operation_endpoint(config, operation))
    val timeout = _positive_millis(config.timeout, 5000L)
    val budget = TimeUnit.MILLISECONDS.toNanos(timeout)
    val started = java.lang.System.nanoTime()
    val query = _parameters(config, report, snapshot, state).map { case (key, value) => s"${_encode(key)}=${_encode(value)}" }.mkString("?", "&", "")
    val uri = URI.create(endpoint.toString + query)
    if (!Set("http", "https").contains(Option(uri.getScheme).getOrElse("").toLowerCase) || uri.getHost == null)
      throw new IOException("notification endpoint invalid")
    val host = uri.getHost
    val authority = host + (if (uri.getPort >= 0) s":${uri.getPort}" else "")
    val target = Option(uri.getRawPath).filter(_.nonEmpty).getOrElse("/") + Option(uri.getRawQuery).map("?" + _).getOrElse("")
    val authorization = s"Bearer $token"
    if (Vector(authority, target, authorization).exists(value => value.contains("\r") || value.contains("\n")))
      throw new IOException("notification header invalid")
    val request = s"GET $target HTTP/1.1\r\nHost: $authority\r\nAuthorization: $authorization\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1)
    val selector = Selector.open()
    try {
      val channel = SocketChannel.open()
      try new DeadlineGet(uri, request, started + budget, started + 2L * budget, budget, channel, selector).run()
      finally channel.close()
    } finally selector.close()
  }

  // CPB-CC4-S2-003: one direct connection, with monotonic bounds through full headers.
  private final class DeadlineGet(
    endpoint: URI,
    request: Array[Byte],
    connectdeadline: Long,
    totaldeadline: Long,
    budget: Long,
    channel: SocketChannel,
    selector: Selector
  ) {
    private val _host = endpoint.getHost.stripPrefix("[").stripSuffix("]")
    private val _secure = endpoint.getScheme.equalsIgnoreCase("https")
    private val _port = if (endpoint.getPort >= 0) endpoint.getPort else if (_secure) 443 else 80
    private val _engine = Option.when(_secure) {
      val engine = SSLContext.getDefault.createSSLEngine(_host, _port)
      engine.setUseClientMode(true)
      val parameters = engine.getSSLParameters
      parameters.setEndpointIdentificationAlgorithm("HTTPS")
      if (!_host.contains(":") && !_host.matches("[0-9.]+"))
        parameters.setServerNames(java.util.List.of(new SNIHostName(_host)))
      engine.setSSLParameters(parameters)
      engine
    }
    private var _network_input = ByteBuffer.allocate(_engine.map(_.getSession.getPacketBufferSize).getOrElse(1))
    private var _plain_input = ByteBuffer.allocate(_engine.map(_.getSession.getApplicationBufferSize).getOrElse(4096))
    private val _tls_gate = new Object
    private var _tls_work_pending = false
    private var _abandoned = false
    private var _engine_cleanup_claimed = false

    def run(): Int = {
      try {
        val resolution = ForkJoinPool.commonPool().submit(new Callable[InetAddress] {
          def call(): InetAddress = InetAddress.getByName(_host)
        })
        val address = _await_task(resolution, connectdeadline)
        channel.configureBlocking(false)
        channel.register(selector, 0)
        if (!channel.connect(new InetSocketAddress(address, _port))) {
          while (!channel.finishConnect()) _wait_ready(SelectionKey.OP_CONNECT, connectdeadline)
        }
        _remaining(connectdeadline)
        _engine.foreach { engine =>
          engine.beginHandshake()
          _handshake(connectdeadline)
        }
        val responsedeadline = math.min(totaldeadline, java.lang.System.nanoTime() + budget)
        val payload = ByteBuffer.wrap(request)
        _engine match {
          case None => _write_channel(payload, responsedeadline)
          case Some(engine) =>
            while (payload.hasRemaining) {
              _handshake(responsedeadline)
              _wrap(payload, responsedeadline)
            }
            _handshake(responsedeadline)
        }
        _read_headers(responsedeadline)
      } finally {
        val cleanup = _tls_gate.synchronized {
          _abandoned = true
          _claim_engine_cleanup()
        }
        if (cleanup) _close_engine()
      }
    }

    // Called only under _tls_gate; engine operations remain outside the gate.
    private def _claim_engine_cleanup(): Boolean =
      if (_abandoned && !_tls_work_pending && !_engine_cleanup_claimed) {
        _engine_cleanup_claimed = true
        true
      } else false

    private def _finish_tls_work(): Unit = {
      val cleanup = _tls_gate.synchronized {
        _tls_work_pending = false
        _claim_engine_cleanup()
      }
      if (cleanup) _close_engine()
    }

    private def _close_engine(): Unit =
      _engine.foreach { engine =>
        try engine.closeOutbound() catch { case NonFatal(_) => () }
        try engine.closeInbound() catch { case NonFatal(_) => () }
      }

    private def _remaining(deadline: Long): Long = {
      if (Thread.currentThread().isInterrupted) throw new IOException("notification interrupted")
      val remaining = deadline - java.lang.System.nanoTime()
      if (remaining <= 0L) throw new SocketTimeoutException("notification deadline")
      remaining
    }

    private def _await_task[A](task: Future[A], deadline: Long, cancel: Boolean = true): A =
      try task.get(_remaining(deadline), TimeUnit.NANOSECONDS)
      catch {
        case error: InterruptedException =>
          if (cancel) task.cancel(true)
          Thread.currentThread().interrupt()
          throw error
        case NonFatal(error) =>
          if (cancel) task.cancel(true)
          throw error
      }

    private def _wait_ready(interest: Int, deadline: Long): Unit = {
      channel.keyFor(selector).interestOps(interest)
      selector.selectedKeys().clear()
      selector.select(math.max(1L, TimeUnit.NANOSECONDS.toMillis(_remaining(deadline))))
      _remaining(deadline)
      selector.selectedKeys().clear()
    }

    private def _write_channel(buffer: ByteBuffer, deadline: Long): Unit = {
      while (buffer.hasRemaining) {
        _remaining(deadline)
        if (channel.write(buffer) == 0) _wait_ready(SelectionKey.OP_WRITE, deadline)
      }
    }

    private def _read_channel(buffer: ByteBuffer, deadline: Long): Unit = {
      var received = 0
      while (received == 0) {
        _remaining(deadline)
        received = channel.read(buffer)
        if (received < 0) throw new EOFException("notification response incomplete")
        if (received == 0) _wait_ready(SelectionKey.OP_READ, deadline)
      }
    }

    private def _grow(buffer: ByteBuffer, minimum: Int): ByteBuffer = {
      val expanded = ByteBuffer.allocate(math.max(buffer.capacity() * 2, minimum))
      buffer.flip()
      expanded.put(buffer)
      expanded
    }

    private def _wrap(payload: ByteBuffer, deadline: Long): Unit = {
      val engine = _engine.get
      var output = ByteBuffer.allocate(engine.getSession.getPacketBufferSize)
      var wrapped = false
      while (!wrapped) {
        _remaining(deadline)
        val result = engine.wrap(payload, output)
        result.getStatus match {
          case SSLEngineResult.Status.BUFFER_OVERFLOW =>
            output = _grow(output, engine.getSession.getPacketBufferSize)
          case SSLEngineResult.Status.OK =>
            output.flip()
            _write_channel(output, deadline)
            wrapped = true
          case _ => throw new IOException("notification TLS write failed")
        }
      }
    }

    private def _unwrap(deadline: Long): Unit = {
      val engine = _engine.get
      var unwrapped = false
      while (!unwrapped) {
        _remaining(deadline)
        _network_input.flip()
        val result = try engine.unwrap(_network_input, _plain_input) finally _network_input.compact()
        result.getStatus match {
          case SSLEngineResult.Status.BUFFER_OVERFLOW =>
            _plain_input = _grow(_plain_input, engine.getSession.getApplicationBufferSize)
          case SSLEngineResult.Status.BUFFER_UNDERFLOW =>
            if (!_network_input.hasRemaining)
              _network_input = _grow(_network_input, engine.getSession.getPacketBufferSize)
            _read_channel(_network_input, deadline)
          case SSLEngineResult.Status.OK => unwrapped = true
          case _ => throw new EOFException("notification TLS response incomplete")
        }
      }
    }

    private def _handshake(deadline: Long): Unit = {
      val engine = _engine.get
      var complete = false
      while (!complete) {
        _remaining(deadline)
        engine.getHandshakeStatus match {
          case SSLEngineResult.HandshakeStatus.NEED_TASK =>
            var task = engine.getDelegatedTask
            while (task != null) {
              val delegated = task
              _tls_gate.synchronized { _tls_work_pending = true }
              val pending = try {
                ForkJoinPool.commonPool().submit(new Callable[Unit] {
                  def call(): Unit =
                    try delegated.run() finally _finish_tls_work()
                })
              } catch {
                case NonFatal(error) =>
                  _finish_tls_work()
                  throw error
              }
              _await_task(pending, deadline, cancel = false)
              task = engine.getDelegatedTask
            }
          case SSLEngineResult.HandshakeStatus.NEED_WRAP =>
            _wrap(ByteBuffer.allocate(0), deadline)
          case SSLEngineResult.HandshakeStatus.NEED_UNWRAP | SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN =>
            _unwrap(deadline)
          case _ => complete = true
        }
      }
    }

    private def _read_headers(deadline: Long): Int = {
      val header = new ByteArrayOutputStream()
      val terminator = Array[Byte](13, 10, 13, 10)
      var matched = 0
      var received = 0
      while (true) {
        _remaining(deadline)
        if (_plain_input.position() == 0) {
          _engine match {
            case None => _read_channel(_plain_input, deadline)
            case Some(_) =>
              _handshake(deadline)
              if (_plain_input.position() == 0) _unwrap(deadline)
          }
        }
        _plain_input.flip()
        while (_plain_input.hasRemaining) {
          _remaining(deadline)
          val next = _plain_input.get()
          received += 1
          if (received > 65536) throw new IOException("notification headers too large")
          header.write(next.toInt & 0xff)
          matched = if (next == terminator(matched)) matched + 1 else if (next == 13) 1 else 0
          if (matched == 4) {
            val lines = header.toString(StandardCharsets.ISO_8859_1).split("\r\n", -1)
            if (!lines.head.matches("HTTP/1\\.[01] [1-5][0-9]{2}(?:[ \\t].*)?"))
              throw new IOException("notification status invalid")
            if (lines.drop(1).dropRight(2).exists { line =>
              val colon = line.indexOf(':')
              colon <= 0 || !line.take(colon).matches("[!#$%&'*+.^_\u0060|~0-9A-Za-z-]+")
            }) throw new IOException("notification headers invalid")
            val status = lines.head.substring(9, 12).toInt
            if (status >= 200) return status
            header.reset()
            matched = 0
          }
        }
        _plain_input.compact()
      }
      throw new EOFException("notification response incomplete")
    }
  }

  private def _parameters(
    config: TextusControlCenterRegistrationConfig,
    report: TextusControlCenterRegistrationReport,
    snapshot: RegistrationSnapshot,
    state: String
  ): Vector[(String, String)] =
    Vector(
      "protocolVersion" -> "1",
      "instanceId" -> report.instanceId,
      "launcherKind" -> "textus",
      "target" -> report.target,
      "executionMode" -> report.executionMode,
      "runtimeVersion" -> report.runtimeVersion,
      "baseUrl" -> snapshot.baseurl,
      "hostLabel" -> config.hostLabel,
      "startedAt" -> report.startedAt.toString,
      "launcherState" -> state
    ) ++ snapshot.applicationurl.map("applicationUrl" -> _).toVector ++ report.artifactId.map("artifactId" -> _).toVector ++ report.developmentDirectory.map("developmentDirectory" -> _).toVector ++ report.subsystemName.map("subsystemName" -> _).toVector ++ report.subsystemVersion.map("subsystemVersion" -> _).toVector

  private def _snapshot(config: TextusControlCenterRegistrationConfig): Option[RegistrationSnapshot] =
    sys.props.get(_bound_snapshot_property_key).flatMap(_parse_bound_snapshot).flatMap { bound =>
      val publicbase = Option(config.baseUrl).map(_.trim).filter(_.nonEmpty).getOrElse(bound.baseurl)
      _valid_public_base(publicbase).map { baseurl =>
        val applicationurl = bound.applicationpath.flatMap(path => _application_url(baseurl, path))
        RegistrationSnapshot(baseurl, applicationurl)
      }
    }

  private def _parse_bound_snapshot(value: String): Option[BoundServerSnapshot] =
    value.split("\\n", -1).toVector match {
      case Vector("v1", baseurl, "") =>
        _valid_public_base(baseurl).map(value => BoundServerSnapshot(value, None))
      case Vector("v1", baseurl, path) =>
        for {
          validbase <- _valid_public_base(baseurl)
          validpath <- _valid_application_path(path)
        } yield BoundServerSnapshot(validbase, Some(validpath))
      case _ => None
    }

  private def _valid_public_base(value: String): Option[String] =
    scala.util.Try(URI.create(value)).toOption.filter { uri =>
      uri.isAbsolute &&
        Option(uri.getScheme).exists(scheme => Set("http", "https").contains(scheme.toLowerCase)) &&
        Option(uri.getHost).exists(_.nonEmpty) &&
        uri.getUserInfo == null && uri.getQuery == null && uri.getFragment == null &&
        Set("", "/").contains(Option(uri.getPath).getOrElse(""))
    }.map(_ => value.stripSuffix("/"))

  private def _application_url(baseurl: String, path: String): Option[String] =
    _valid_application_path(path).map { validpath =>
      s"${baseurl.stripSuffix("/")}$validpath"
    }

  private def _valid_application_path(path: String): Option[String] =
    Option.when(
      path == "/web" ||
        (path.startsWith("/web/") &&
          path != "/web/system" &&
          !path.startsWith("/web/system/") &&
          !path.contains("//") &&
          !path.contains("?") &&
          !path.contains("#") &&
          !path.split("/", -1).drop(1).exists(segment => segment.isEmpty || segment == "." || segment == ".."))
    ) {
      path
    }

  private def _operation_endpoint(
    config: TextusControlCenterRegistrationConfig,
    operation: String
  ): String =
    config.endpoint.stripSuffix("/") + "/" + operation

  private def _encode(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8)

  private def _daemon_thread_factory = new java.util.concurrent.ThreadFactory {
    def newThread(runnable: Runnable): Thread = {
      val thread = new Thread(runnable, "textus-control-center-registration-heartbeat")
      thread.setDaemon(true)
      thread
    }
  }

  private def _warning(message: String): Unit =
    Console.err.println(s"warning: Textus Control Center registration $message; continuing server startup.")

  private val _bound_snapshot_property_key = "textus.server.bound-snapshot"
}

private final case class BoundServerSnapshot(
  baseurl: String,
  applicationpath: Option[String]
)

private final case class RegistrationSnapshot(
  baseurl: String,
  applicationurl: Option[String]
)

object TextusControlCenterRegistrationSession {
  val noop: TextusControlCenterRegistrationSession = new TextusControlCenterRegistrationSession {
    def close(): Unit = ()
  }
}

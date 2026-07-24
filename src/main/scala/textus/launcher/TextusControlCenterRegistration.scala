package textus.launcher

import java.net.{HttpURLConnection, URI, URLEncoder}
import java.nio.charset.StandardCharsets
import java.time.{Duration, Instant}
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

/*
 * @since   Jul. 18, 2026
 * @version Jul. 24, 2026
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
        if (config.baseUrl.trim.nonEmpty)
          _start_active(config, report, value)
        else
          _start_pending(config, report, value)
      case Some(_) =>
        _warning("credential is unavailable")
        TextusControlCenterRegistrationSession.noop
    }
  }

  private def _start_active(
    config: TextusControlCenterRegistrationConfig,
    report: TextusControlCenterRegistrationReport,
    token: String
  ): TextusControlCenterRegistrationSession = {
    val registered = new AtomicBoolean(_post_best_effort(config, report, token, "register-subsystem", "starting"))
    val executor = Executors.newSingleThreadScheduledExecutor(_daemon_thread_factory)
    val task = new Runnable {
      def run(): Unit =
        if (registered.get) {
          if (!_post_best_effort(config, report, token, "heartbeat-subsystem", "running"))
            registered.set(false)
        } else {
          registered.set(_post_best_effort(config, report, token, "register-subsystem", "starting"))
        }
    }
    executor.scheduleAtFixedRate(task, config.heartbeatInterval.toMillis, config.heartbeatInterval.toMillis, TimeUnit.MILLISECONDS)
    ActiveTextusControlCenterRegistrationSession(
      executor,
      () => if (registered.get) _post_best_effort(config, report, token, "deregister-subsystem", "stopped")
    )
  }

  private def _start_pending(
    config: TextusControlCenterRegistrationConfig,
    report: TextusControlCenterRegistrationReport,
    token: String
  ): TextusControlCenterRegistrationSession = {
    val executor = Executors.newSingleThreadScheduledExecutor(_daemon_thread_factory)
    val closed = new AtomicBoolean(false)
    val activated = new AtomicBoolean(false)
    val active = new AtomicReference[TextusControlCenterRegistrationSession](TextusControlCenterRegistrationSession.noop)
    val task = new Runnable {
      def run(): Unit =
        sys.props.get(_bound_base_url_property_key).map(_.trim).filter(_.nonEmpty).foreach { baseurl =>
          if (!closed.get && activated.compareAndSet(false, true)) {
            val session = _start_active(config.copy(baseUrl = baseurl), report, token)
            active.set(session)
            executor.shutdown()
            if (closed.get) session.close()
          }
        }
    }
    executor.scheduleWithFixedDelay(task, 0L, 50L, TimeUnit.MILLISECONDS)
    new TextusControlCenterRegistrationSession {
      def close(): Unit =
        if (closed.compareAndSet(false, true)) {
          executor.shutdownNow()
          active.get.close()
        }
    }
  }

  private def _post_best_effort(
    config: TextusControlCenterRegistrationConfig,
    report: TextusControlCenterRegistrationReport,
    token: String,
    operation: String,
    state: String
  ): Boolean =
    try {
      val status = _post(config, report, token, operation, state)
      if (status < 200 || status >= 300) {
        _warning(s"$operation request to ${_operation_endpoint(config, operation)} returned HTTP $status")
        false
      } else {
        true
      }
    } catch {
      case _: Throwable =>
        _warning(s"$operation connection to ${_operation_endpoint(config, operation)} failed")
        false
    }

  private def _post(
    config: TextusControlCenterRegistrationConfig,
    report: TextusControlCenterRegistrationReport,
    token: String,
    operation: String,
    state: String
  ): Int = {
    val endpoint = URI.create(_operation_endpoint(config, operation))
    val timeout = config.timeout.toMillis.min(Int.MaxValue.toLong).toInt
    val query = _parameters(config, report, state).map { case (key, value) => s"${_encode(key)}=${_encode(value)}" }.mkString("?", "&", "")
    val connection = URI.create(endpoint.toString + query).toURL.openConnection().asInstanceOf[HttpURLConnection]
    try {
      connection.setRequestMethod("GET")
      connection.setConnectTimeout(timeout)
      connection.setReadTimeout(timeout)
      connection.setRequestProperty("Authorization", s"Bearer $token")
      connection.getResponseCode
    } finally {
      connection.disconnect()
    }
  }

  private def _parameters(
    config: TextusControlCenterRegistrationConfig,
    report: TextusControlCenterRegistrationReport,
    state: String
  ): Vector[(String, String)] =
    Vector(
      "protocolVersion" -> "1",
      "instanceId" -> report.instanceId,
      "launcherKind" -> "textus",
      "target" -> report.target,
      "executionMode" -> report.executionMode,
      "runtimeVersion" -> report.runtimeVersion,
      "baseUrl" -> config.baseUrl,
      "hostLabel" -> config.hostLabel,
      "startedAt" -> report.startedAt.toString,
      "launcherState" -> state
    ) ++ report.artifactId.map("artifactId" -> _).toVector ++ report.developmentDirectory.map("developmentDirectory" -> _).toVector ++ report.subsystemName.map("subsystemName" -> _).toVector ++ report.subsystemVersion.map("subsystemVersion" -> _).toVector

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

  private val _bound_base_url_property_key = "textus.server.bound-base-url"
}

private final case class ActiveTextusControlCenterRegistrationSession(
  executor: ScheduledExecutorService,
  closeAction: () => Unit,
  closed: AtomicBoolean = new AtomicBoolean(false)
) extends TextusControlCenterRegistrationSession {
  def close(): Unit =
    if (closed.compareAndSet(false, true)) {
      executor.shutdownNow()
      closeAction()
    }
}

object TextusControlCenterRegistrationSession {
  val noop: TextusControlCenterRegistrationSession = new TextusControlCenterRegistrationSession {
    def close(): Unit = ()
  }
}

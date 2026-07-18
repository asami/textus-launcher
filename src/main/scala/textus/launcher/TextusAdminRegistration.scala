package textus.launcher

import java.net.{HttpURLConnection, URI, URLEncoder}
import java.nio.charset.StandardCharsets
import java.time.{Duration, Instant}
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

/*
 * @since   Jul. 18, 2026
 * @version Jul. 18, 2026
 * @author  ASAMI, Tomoharu
 */
final case class TextusAdminRegistrationConfig(
  endpoint: String,
  tokenEnv: String,
  timeout: Duration,
  heartbeatInterval: Duration,
  hostLabel: String,
  baseUrl: String
)

object TextusAdminRegistrationConfig {
  def enabled(values: Map[String, Vector[String]]): Option[Boolean] =
    _first(values, "textus-admin.registration.enabled", "textus.admin.registration.enabled").map(_boolean)

  def fromParsed(values: Map[String, Vector[String]]): Option[TextusAdminRegistrationConfig] = {
    def _first_(keys: String*): Option[String] =
      _first(values, keys*)

    if (!enabled(values).contains(true)) {
      None
    } else {
      Some(TextusAdminRegistrationConfig(
        _first_("textus-admin.registration.endpoint", "textus.admin.registration.endpoint").getOrElse(throw TextusException("textus-admin.registration.endpoint is required when registration is enabled")),
        _first_("textus-admin.registration.token-env", "textus-admin.registration.tokenEnv", "textus.admin.registration.token-env", "textus.admin.registration.tokenEnv").getOrElse(throw TextusException("textus-admin.registration.token-env is required when registration is enabled")),
        _duration(_first_("textus-admin.registration.timeout", "textus.admin.registration.timeout").getOrElse("2s"), "textus-admin.registration.timeout"),
        _duration(_first_("textus-admin.registration.heartbeat-interval", "textus-admin.registration.heartbeatInterval", "textus.admin.registration.heartbeat-interval", "textus.admin.registration.heartbeatInterval").getOrElse("30s"), "textus-admin.registration.heartbeat-interval"),
        _first_("textus-admin.registration.host-label", "textus-admin.registration.hostLabel", "textus.admin.registration.host-label", "textus.admin.registration.hostLabel").getOrElse(throw TextusException("textus-admin.registration.host-label is required when registration is enabled")),
        _first_("textus-admin.registration.base-url", "textus-admin.registration.baseUrl", "textus.admin.registration.base-url", "textus.admin.registration.baseUrl").getOrElse(throw TextusException("textus-admin.registration.base-url is required when registration is enabled"))
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

final case class TextusAdminRegistrationReport(
  instanceId: String,
  target: String,
  subsystemName: Option[String],
  subsystemVersion: Option[String],
  runtimeVersion: String,
  startedAt: Instant
)

trait TextusAdminRegistrationSession {
  def close(): Unit
}

trait TextusAdminRegistrationReporter {
  def start(
    config: TextusAdminRegistrationConfig,
    report: TextusAdminRegistrationReport,
    token: Option[String]
  ): TextusAdminRegistrationSession
}

object TextusAdminRegistrationReporter {
  val System: TextusAdminRegistrationReporter = new SystemTextusAdminRegistrationReporter
}

private final class SystemTextusAdminRegistrationReporter extends TextusAdminRegistrationReporter {
  def start(
    config: TextusAdminRegistrationConfig,
    report: TextusAdminRegistrationReport,
    token: Option[String]
  ): TextusAdminRegistrationSession = {
    token match {
      case None =>
        _warning("credential is unavailable")
        TextusAdminRegistrationSession.noop
      case Some(value) if value.trim.nonEmpty =>
        _post_best_effort(config, report, value, "register-subsystem", "starting")
        val executor = Executors.newSingleThreadScheduledExecutor(_daemon_thread_factory)
        val task = new Runnable {
          def run(): Unit =
            _post_best_effort(config, report, value, "heartbeat-subsystem", "running")
        }
        executor.scheduleAtFixedRate(task, config.heartbeatInterval.toMillis, config.heartbeatInterval.toMillis, TimeUnit.MILLISECONDS)
        ActiveTextusAdminRegistrationSession(
          executor,
          () => _post_best_effort(config, report, value, "deregister-subsystem", "stopped")
        )
      case Some(_) =>
        _warning("credential is unavailable")
        TextusAdminRegistrationSession.noop
    }
  }

  private def _post_best_effort(
    config: TextusAdminRegistrationConfig,
    report: TextusAdminRegistrationReport,
    token: String,
    operation: String,
    state: String
  ): Unit =
    try {
      val status = _post(config, report, token, operation, state)
      if (status < 200 || status >= 300)
        _warning(s"$operation request to ${_operation_endpoint(config, operation)} returned HTTP $status")
    } catch {
      case _: Throwable => _warning(s"$operation connection to ${_operation_endpoint(config, operation)} failed")
    }

  private def _post(
    config: TextusAdminRegistrationConfig,
    report: TextusAdminRegistrationReport,
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
    config: TextusAdminRegistrationConfig,
    report: TextusAdminRegistrationReport,
    state: String
  ): Vector[(String, String)] =
    Vector(
      "protocolVersion" -> "1",
      "instanceId" -> report.instanceId,
      "launcherKind" -> "textus",
      "target" -> report.target,
      "runtimeVersion" -> report.runtimeVersion,
      "baseUrl" -> config.baseUrl,
      "hostLabel" -> config.hostLabel,
      "startedAt" -> report.startedAt.toString,
      "launcherState" -> state
    ) ++ report.subsystemName.map("subsystemName" -> _).toVector ++ report.subsystemVersion.map("subsystemVersion" -> _).toVector

  private def _operation_endpoint(
    config: TextusAdminRegistrationConfig,
    operation: String
  ): String =
    config.endpoint.stripSuffix("/") + "/" + operation

  private def _encode(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8)

  private def _daemon_thread_factory = new java.util.concurrent.ThreadFactory {
    def newThread(runnable: Runnable): Thread = {
      val thread = new Thread(runnable, "textus-admin-registration-heartbeat")
      thread.setDaemon(true)
      thread
    }
  }

  private def _warning(message: String): Unit =
    Console.err.println(s"warning: Textus Admin registration $message; continuing server startup.")
}

private final case class ActiveTextusAdminRegistrationSession(
  executor: ScheduledExecutorService,
  closeAction: () => Unit
) extends TextusAdminRegistrationSession {
  def close(): Unit = {
    executor.shutdownNow()
    closeAction()
  }
}

object TextusAdminRegistrationSession {
  val noop: TextusAdminRegistrationSession = new TextusAdminRegistrationSession {
    def close(): Unit = ()
  }
}

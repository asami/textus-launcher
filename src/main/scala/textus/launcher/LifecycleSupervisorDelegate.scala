package textus.launcher

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Duration

/*
 * @version Jul. 22, 2026
 */
trait LifecycleSupervisorDelegate {
  def submit(endpoint: URI, token: String, requestjson: String, timeout: Duration): Either[String, String]
  def lookup(endpoint: URI, token: String, requestid: String, timeout: Duration): Either[String, String]
}

object LifecycleSupervisorDelegate {
  object System extends LifecycleSupervisorDelegate {
    def submit(endpoint: URI, token: String, requestjson: String, timeout: Duration): Either[String, String] =
      _request(endpoint, token, timeout, "supervisor-request-rejected") { base =>
        HttpRequest.newBuilder(base.resolve("/v1/lifecycle-requests"))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(requestjson))
      }

    def lookup(endpoint: URI, token: String, requestid: String, timeout: Duration): Either[String, String] =
      if (requestid.trim.isEmpty) Left("supervisor-request-invalid")
      else _request(endpoint, token, timeout, "supervisor-request-unavailable") { base =>
        HttpRequest.newBuilder(base.resolve(s"/v1/lifecycle-requests/${java.net.URLEncoder.encode(requestid, java.nio.charset.StandardCharsets.UTF_8)}"))
          .GET()
      }

    private def _request(
      endpoint: URI,
      token: String,
      timeout: Duration,
      failurecode: String
    )(
      build: URI => HttpRequest.Builder
    ): Either[String, String] =
      _validate(endpoint, token, timeout).flatMap { _ =>
        try {
          val request = build(endpoint)
            .timeout(timeout)
            .header("Authorization", s"Bearer $token")
            .build()
          val response = HttpClient.newBuilder().connectTimeout(timeout).build().send(request, HttpResponse.BodyHandlers.ofString())
          if (response.statusCode() >= 200 && response.statusCode() < 300) Right(response.body())
          else Left(failurecode)
        } catch {
          case _: java.net.http.HttpTimeoutException => Left("supervisor-request-timed-out")
          case _: Throwable => Left("supervisor-unreachable")
        }
      }

    private def _validate(endpoint: URI, token: String, timeout: Duration): Either[String, Unit] =
      if (!Option(endpoint.getScheme).exists(_.equalsIgnoreCase("http")) || !_loopback(endpoint.getHost))
        Left("supervisor-protocol-unavailable")
      else if (token.trim.isEmpty || timeout.isZero || timeout.isNegative)
        Left("supervisor-request-invalid")
      else Right(())

    private def _loopback(host: String): Boolean =
      Option(host).exists(value => Set("127.0.0.1", "::1", "[::1]", "localhost").contains(value.toLowerCase))
  }
}

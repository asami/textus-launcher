package textus.launcher

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Duration

/*
 * @version Jul. 22, 2026
 */
trait LifecycleSupervisorDelegate {
  def submit(endpoint: URI, token: String, requestjson: String, timeout: Duration): Either[String, String]
}

object LifecycleSupervisorDelegate {
  object System extends LifecycleSupervisorDelegate {
    def submit(endpoint: URI, token: String, requestjson: String, timeout: Duration): Either[String, String] =
      try {
        val request = HttpRequest.newBuilder(endpoint.resolve("/v1/lifecycle-requests"))
          .timeout(timeout)
          .header("Authorization", s"Bearer $token")
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(requestjson))
          .build()
        val response = HttpClient.newBuilder().connectTimeout(timeout).build().send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() >= 200 && response.statusCode() < 300) Right(response.body())
        else Left("supervisor-request-rejected")
      } catch {
        case _: java.net.http.HttpTimeoutException => Left("supervisor-request-timed-out")
        case _: Throwable => Left("supervisor-unreachable")
      }
  }
}

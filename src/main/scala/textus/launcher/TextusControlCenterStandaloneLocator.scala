package textus.launcher

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.nio.file.attribute.PosixFilePermission
import java.time.Duration

/*
 * @since   Jul. 19, 2026
 * @version Jul. 19, 2026
 * @author  ASAMI, Tomoharu
 */
final case class TextusControlCenterStandaloneRegistration(
  config: TextusControlCenterRegistrationConfig,
  token: String
)

object TextusControlCenterStandaloneLocator {
  def resolve(paths: LauncherPaths): Option[TextusControlCenterStandaloneRegistration] =
    scala.util.Try(_resolve(paths)).toOption.flatten

  private def _resolve(paths: LauncherPaths): Option[TextusControlCenterStandaloneRegistration] = {
    val root = paths.cncfHome.resolve("textus-control-center").normalize
    val locator = root.resolve("standalone-locator.yaml").normalize
    if (!Files.isRegularFile(locator) || Files.isSymbolicLink(locator) || !locator.startsWith(root)) {
      None
    } else {
      val values = _values(locator)
      for {
        _ <- Option.when(values.get("schemaVersion").contains("1"))(())
        _ <- Option.when(values.get("profile").contains("standalone"))(())
        _ <- values.get("scopeId").filter(_.matches("[A-Za-z0-9._-]+"))
        _ <- values.get("installationId").filter(_.matches("[A-Za-z0-9._-]+"))
        endpoint <- values.get("endpoint").filter(_is_loopback_endpoint)
        credentialref <- values.get("credentialRef")
        credential <- _credential(root, credentialref)
        token <- _token(credential)
        timeout <- _duration(values.getOrElse("timeout", "2s"))
        heartbeatinterval <- _duration(values.getOrElse("heartbeatInterval", "30s"))
        hostlabel <- values.get("hostLabel").filter(_.matches("[A-Za-z0-9._-]+"))
      } yield TextusControlCenterStandaloneRegistration(
        TextusControlCenterRegistrationConfig(
          endpoint = endpoint,
          tokenEnv = "",
          timeout = timeout,
          heartbeatInterval = heartbeatinterval,
          hostLabel = hostlabel,
          baseUrl = ""
        ),
        token
      )
    }
  }

  private def _values(path: Path): Map[String, String] =
    Files.readAllLines(path, StandardCharsets.UTF_8).toArray(new Array[String](0)).iterator.flatMap { line =>
      line.split(":", 2).toList match {
        case key :: value :: Nil => Some(key.trim -> value.trim)
        case _ => None
      }
    }.toMap

  private def _credential(root: Path, reference: String): Option[Path] = {
    val relative = Path.of(reference)
    val path = root.resolve(relative).normalize
    Option.when(
      !relative.isAbsolute &&
        relative.startsWith(Path.of("credentials")) &&
        path.startsWith(root) &&
        Files.isRegularFile(path) &&
        !Files.isSymbolicLink(path) &&
        _is_owner_only(path)
    )(path)
  }

  private def _token(path: Path): Option[String] =
    Option(Files.readString(path, StandardCharsets.UTF_8).trim).filter(_.nonEmpty)

  private def _duration(value: String): Option[Duration] = {
    val normalized = value.trim
    val duration =
      if (normalized.matches("[0-9]+ms")) scala.util.Try(Duration.ofMillis(normalized.stripSuffix("ms").toLong)).toOption
      else if (normalized.matches("[0-9]+s")) scala.util.Try(Duration.ofSeconds(normalized.stripSuffix("s").toLong)).toOption
      else if (normalized.matches("[0-9]+m")) scala.util.Try(Duration.ofMinutes(normalized.stripSuffix("m").toLong)).toOption
      else scala.util.Try(Duration.parse(normalized)).toOption
    duration.filter(value => !value.isZero && !value.isNegative)
  }

  private def _is_loopback_endpoint(value: String): Boolean =
    scala.util.Try(URI.create(value)).toOption.exists { uri =>
      uri.getScheme == "http" &&
        Set("127.0.0.1", "localhost", "::1").contains(Option(uri.getHost).getOrElse("")) &&
        uri.getPort > 0 &&
        uri.getUserInfo == null &&
        Option(uri.getPath).contains("/rest/v1/textus-control-center/subsystem-inventory") &&
        uri.getQuery == null &&
        uri.getFragment == null
    }

  private def _is_owner_only(path: Path): Boolean =
    scala.util.Try(Files.getPosixFilePermissions(path)).toOption.exists { permissions =>
      permissions == java.util.Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
    }
}

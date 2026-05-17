package textus.launcher

import java.net.URI
import java.nio.file.{Files, Path, Paths}
import scala.util.Using

/*
 * @since   May. 17, 2026
 * @version May. 17, 2026
 * @author  ASAMI, Tomoharu
 */
final case class ResolvedArtifact(
  selector: ArtifactSelector,
  kind: ArtifactKind
)

final class ArtifactResolver {
  def resolve(selector: ArtifactSelector, config: LauncherConfig): ResolvedArtifact =
    selector.kind match {
      case ArtifactKind.Car =>
        if (_exists(selector, config.carRepositories, ".car"))
          ResolvedArtifact(selector, ArtifactKind.Car)
        else
          throw TextusException(s"CAR artifact not found in repositories: ${selector.display}")
      case ArtifactKind.Sar =>
        if (_exists(selector, config.sarRepositories, ".sar"))
          ResolvedArtifact(selector, ArtifactKind.Sar)
        else
          throw TextusException(s"SAR artifact not found in repositories: ${selector.display}")
      case ArtifactKind.Auto =>
        val car = _exists(selector, config.carRepositories, ".car")
        val sar = _exists(selector, config.sarRepositories, ".sar")
        (car, sar) match {
          case (true, false) => ResolvedArtifact(selector.copy(kind = ArtifactKind.Car), ArtifactKind.Car)
          case (false, true) => ResolvedArtifact(selector.copy(kind = ArtifactKind.Sar), ArtifactKind.Sar)
          case (true, true) =>
            throw TextusException(s"artifact is ambiguous between CAR and SAR: ${selector.display}; use --car or --sar")
          case (false, false) =>
            throw TextusException(s"artifact not found in CAR/SAR repositories: ${selector.display}")
        }
    }

  private def _exists(
    selector: ArtifactSelector,
    repositories: Vector[String],
    suffix: String
  ): Boolean =
    repositories.exists(repo => _exists_in_repository(selector, repo, suffix))

  private def _exists_in_repository(
    selector: ArtifactSelector,
    repository: String,
    suffix: String
  ): Boolean =
    if (_is_url(repository))
      _remote_exists(selector, repository, suffix)
    else
      _local_exists(selector, Paths.get(repository), suffix)

  private def _local_exists(
    selector: ArtifactSelector,
    repository: Path,
    suffix: String
  ): Boolean = {
    val root = repository.resolve(selector.name)
    if (!Files.isDirectory(root)) {
      false
    } else {
      val versions = selector.version.map(Vector(_)).getOrElse(_version_dirs(root))
      versions.exists(v => Files.isRegularFile(root.resolve(v).resolve(s"${selector.name}-$v$suffix")))
    }
  }

  private def _remote_exists(
    selector: ArtifactSelector,
    repository: String,
    suffix: String
  ): Boolean = {
    val versions = selector.version.map(Vector(_)).getOrElse(_remote_versions(repository, selector.name))
    versions.exists { v =>
      val url = _join(repository, selector.name, v, s"${selector.name}-$v$suffix")
      _head(url)
    }
  }

  private def _remote_versions(repository: String, name: String): Vector[String] = {
    val url = _join(repository, name, "maven-metadata.xml")
    try {
      val connection = URI.create(url).toURL.openConnection()
      connection.setConnectTimeout(2000)
      connection.setReadTimeout(5000)
      val text = Using.resource(scala.io.Source.fromInputStream(connection.getInputStream, "UTF-8"))(_.mkString)
      val latest = _first_tag(text, "latest").orElse(_first_tag(text, "release")).toVector
      val versions = "<version>([^<]+)</version>".r.findAllMatchIn(text).map(_.group(1).trim).filter(_.nonEmpty).toVector
      (latest ++ versions.reverse).distinct
    } catch {
      case _: Throwable => Vector.empty
    }
  }

  private def _head(url: String): Boolean =
    try {
      val c = URI.create(url).toURL.openConnection().asInstanceOf[java.net.HttpURLConnection]
      c.setRequestMethod("HEAD")
      c.setConnectTimeout(2000)
      c.setReadTimeout(5000)
      val code = c.getResponseCode
      code >= 200 && code < 400
    } catch {
      case _: Throwable => false
    }

  private def _version_dirs(root: Path): Vector[String] = {
    val stream = Files.list(root)
    try {
      import scala.jdk.CollectionConverters.*
      stream.iterator().asScala.filter(Files.isDirectory(_)).map(_.getFileName.toString).toVector.sorted.reverse
    } finally {
      stream.close()
    }
  }

  private def _is_url(value: String): Boolean =
    value.startsWith("http://") || value.startsWith("https://")

  private def _join(parts: String*): String =
    parts.toVector.zipWithIndex.map { case (p, idx) =>
      if (idx == 0) p.reverse.dropWhile(_ == '/').reverse
      else p.dropWhile(_ == '/').reverse.dropWhile(_ == '/').reverse
    }.mkString("/")

  private def _first_tag(text: String, tag: String): Option[String] =
    s"<$tag>([^<]+)</$tag>".r.findFirstMatchIn(text).map(_.group(1).trim).filter(_.nonEmpty)
}

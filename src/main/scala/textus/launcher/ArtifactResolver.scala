package textus.launcher

import java.net.URI
import java.nio.file.{Files, Path, Paths}
import scala.util.Using

/*
 * @since   May. 17, 2026
 * @version May. 20, 2026
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
        _resolve_version(selector, config.carRepositories, ".car") match {
          case Some(version) => ResolvedArtifact(selector.copy(kind = ArtifactKind.Car, version = Some(version)), ArtifactKind.Car)
          case None =>
            throw TextusException(s"CAR artifact not found in repositories: ${selector.display}")
        }
      case ArtifactKind.Sar =>
        _resolve_version(selector, config.sarRepositories, ".sar") match {
          case Some(version) => ResolvedArtifact(selector.copy(kind = ArtifactKind.Sar, version = Some(version)), ArtifactKind.Sar)
          case None =>
            throw TextusException(s"SAR artifact not found in repositories: ${selector.display}")
        }
      case ArtifactKind.Auto =>
        val carversion = _resolve_version(selector, config.carRepositories, ".car")
        val sarversion = _resolve_version(selector, config.sarRepositories, ".sar")
        (carversion, sarversion) match {
          case (Some(version), None) =>
            ResolvedArtifact(selector.copy(kind = ArtifactKind.Car, version = Some(version)), ArtifactKind.Car)
          case (None, Some(version)) =>
            ResolvedArtifact(selector.copy(kind = ArtifactKind.Sar, version = Some(version)), ArtifactKind.Sar)
          case (Some(_), Some(_)) =>
            throw TextusException(s"artifact is ambiguous between CAR and SAR: ${selector.display}; use --car or --sar")
          case (None, None) =>
            throw TextusException(s"artifact not found in CAR/SAR repositories: ${selector.display}")
        }
    }

  private def _resolve_version(
    selector: ArtifactSelector,
    repositories: Vector[String],
    suffix: String
  ): Option[String] =
    repositories.view.flatMap(repo => _resolve_version_in_repository(selector, repo, suffix)).headOption

  private def _resolve_version_in_repository(
    selector: ArtifactSelector,
    repository: String,
    suffix: String
  ): Option[String] =
    if (_is_url(repository))
      _remote_resolve_version(selector, repository, suffix)
    else
      _local_resolve_version(selector, Paths.get(repository), suffix)

  private def _local_resolve_version(
    selector: ArtifactSelector,
    repository: Path,
    suffix: String
  ): Option[String] = {
    val root = repository.resolve(selector.name)
    if (!Files.isDirectory(root)) {
      None
    } else {
      val versions = selector.version.map(Vector(_)).getOrElse(_local_versions(root))
      versions.find(v => Files.isRegularFile(root.resolve(v).resolve(s"${selector.name}-$v$suffix")))
    }
  }

  private def _remote_resolve_version(
    selector: ArtifactSelector,
    repository: String,
    suffix: String
  ): Option[String] = {
    val versions = selector.version.map(Vector(_)).getOrElse(_remote_versions(repository, selector.name))
    versions.find { v =>
      val url = _join(repository, selector.name, v, s"${selector.name}-$v$suffix")
      _head(url)
    }
  }

  private def _local_versions(root: Path): Vector[String] =
    _local_metadata_versions(root).getOrElse(_version_dirs(root))

  private def _local_metadata_versions(root: Path): Option[Vector[String]] = {
    val metadata = root.resolve("maven-metadata.xml")
    if (Files.isRegularFile(metadata)) {
      val text = Files.readString(metadata)
      Some(_metadata_versions(text))
    } else {
      None
    }
  }

  private def _remote_versions(repository: String, name: String): Vector[String] = {
    val url = _join(repository, name, "maven-metadata.xml")
    try {
      val connection = URI.create(url).toURL.openConnection()
      connection.setConnectTimeout(2000)
      connection.setReadTimeout(5000)
      val text = Using.resource(scala.io.Source.fromInputStream(connection.getInputStream, "UTF-8"))(_.mkString)
      _metadata_versions(text)
    } catch {
      case _: Throwable => Vector.empty
    }
  }

  private def _metadata_versions(text: String): Vector[String] = {
    val latest = _first_tag(text, "latest").orElse(_first_tag(text, "release")).toVector
    val versions = "<version>([^<]+)</version>".r.findAllMatchIn(text).map(_.group(1).trim).filter(_.nonEmpty).toVector
    (latest ++ versions.reverse).distinct
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

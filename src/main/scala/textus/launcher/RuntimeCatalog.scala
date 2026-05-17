package textus.launcher

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.util.Using

/*
 * @since   May. 17, 2026
 * @version May. 17, 2026
 * @author  ASAMI, Tomoharu
 */
final case class RuntimeCatalog(
  schemaVersion: String,
  generatedAt: Option[String],
  recommended: Option[String],
  latestStable: Option[String],
  latestSnapshot: Option[String],
  versions: Vector[RuntimeCatalogVersion],
  mavenRepositories: Vector[String],
  carRepositories: Vector[String],
  sarRepositories: Vector[String],
  coursierRepositories: Vector[String]
) {
  def resolve(selector: String): RuntimeCatalogVersion = {
    val version =
      selector match {
        case "recommended" =>
          recommended.getOrElse(throw TextusException("runtime catalog does not define recommended"))
        case "latest" | "latest-stable" | "latest.release" =>
          latestStable.getOrElse(throw TextusException("runtime catalog does not define latestStable"))
        case "latest-snapshot" =>
          latestSnapshot.getOrElse(throw TextusException("runtime catalog does not define latestSnapshot"))
        case "newest" =>
          return newest
        case x =>
          x
      }
    versions.find(_.version == version)
      .getOrElse(throw TextusException(s"runtime version is not listed in catalog: $version"))
      .validated
  }

  def newest: RuntimeCatalogVersion =
    versions
      .filterNot(_.status.contains("disabled"))
      .sortBy(v => (v.publishedAt.getOrElse(""), v.version))
      .lastOption
      .getOrElse(throw TextusException("runtime catalog does not contain an enabled runtime version"))
      .validated

  def renderRemoteList: String =
    versions.map { v =>
      Vector(v.version, v.channel.getOrElse("-"), v.status.getOrElse("active")).mkString("\t")
    }.mkString("\n")

  def renderChannels: String =
    Vector(
      s"recommended: ${recommended.getOrElse("-")}",
      s"latest-stable: ${latestStable.getOrElse("-")}",
      s"latest-snapshot: ${latestSnapshot.getOrElse("-")}",
      s"newest: ${newest.version}"
    ).mkString("\n")

  def render: String = {
    val head = Vector(
      s"schemaVersion: $schemaVersion",
      s"generatedAt: ${generatedAt.getOrElse("")}",
      s"recommended: ${recommended.getOrElse("")}",
      s"latestStable: ${latestStable.getOrElse("")}",
      s"latestSnapshot: ${latestSnapshot.getOrElse("")}"
    )
    val repos = Vector(
      _render_list("mavenRepositories", mavenRepositories),
      _render_list("carRepositories", carRepositories),
      _render_list("sarRepositories", sarRepositories),
      _render_list("coursierRepositories", coursierRepositories)
    )
    val body =
      "versions:" +: versions.flatMap(_.renderLines)
    (head ++ repos ++ body).mkString("\n")
  }

  private def _render_list(name: String, values: Vector[String]): String =
    if (values.isEmpty)
      s"$name: []"
    else
      (s"$name:" +: values.map(v => s"  - $v")).mkString("\n")
}

final case class RuntimeCatalogVersion(
  version: String,
  channel: Option[String],
  status: Option[String],
  scalaBinaryVersion: Option[String],
  module: Option[String],
  publishedAt: Option[String],
  checksumUrl: Option[String],
  metadataUrl: Option[String]
) {
  def validated: RuntimeCatalogVersion =
    status match {
      case Some("disabled") =>
        throw TextusException(s"runtime version is disabled: $version")
      case _ =>
        this
    }

  def warnIfDeprecated(): Unit =
    if (status.contains("deprecated"))
      Console.err.println(s"warning: runtime version is deprecated: $version")

  def moduleCoordinate: String =
    module.getOrElse(s"org.goldenport:goldenport-cncf_3:$version")

  def renderLines: Vector[String] =
    Vector(
      Some(s"  - version: $version"),
      channel.map(v => s"    channel: $v"),
      status.map(v => s"    status: $v"),
      scalaBinaryVersion.map(v => s"    scalaBinaryVersion: $v"),
      Some(s"    module: $moduleCoordinate"),
      publishedAt.map(v => s"    publishedAt: $v"),
      checksumUrl.map(v => s"    checksumUrl: $v"),
      metadataUrl.map(v => s"    metadataUrl: $v")
    ).flatten
}

object RuntimeCatalog {
  def parse(text: String): RuntimeCatalog = {
    var root = Map.empty[String, String]
    var lists = Map.empty[String, Vector[String]]
    var versions = Vector.empty[Map[String, String]]
    var current = Map.empty[String, String]
    var section: Option[String] = None

    def _finish_version_(): Unit =
      if (current.nonEmpty) {
        versions :+= current
        current = Map.empty
      }

    text.linesIterator.foreach { raw =>
      val line = _strip_comment(raw)
      if (line.trim.nonEmpty) {
        val indent = line.takeWhile(_ == ' ').length
        val trimmed = line.trim
        if (indent == 0 && trimmed.endsWith(":")) {
          _finish_version_()
          section = Some(trimmed.dropRight(1))
        } else if (indent == 0 && trimmed.contains(":")) {
          _finish_version_()
          val (key, value) = _split_key_value(trimmed)
          root = root.updated(key, _unquote(value))
          section = None
        } else {
          section match {
            case Some("versions") if trimmed.startsWith("- ") =>
              _finish_version_()
              val rest = trimmed.drop(2).trim
              if (rest.contains(":")) {
                val (key, value) = _split_key_value(rest)
                current = current.updated(key, _unquote(value))
              }
            case Some("versions") if trimmed.contains(":") =>
              val (key, value) = _split_key_value(trimmed)
              current = current.updated(key, _unquote(value))
            case Some(name) if trimmed.startsWith("- ") =>
              val value = _unquote(trimmed.drop(2).trim)
              lists = lists.updated(name, lists.getOrElse(name, Vector.empty) :+ value)
            case _ =>
              ()
          }
        }
      }
    }
    _finish_version_()
    RuntimeCatalog(
      schemaVersion = root.getOrElse("schemaVersion", "1"),
      generatedAt = root.get("generatedAt").filter(_.nonEmpty),
      recommended = root.get("recommended").filter(_.nonEmpty),
      latestStable = root.get("latestStable").filter(_.nonEmpty),
      latestSnapshot = root.get("latestSnapshot").filter(_.nonEmpty),
      versions = versions.flatMap(_version_from_map),
      mavenRepositories = lists.getOrElse("mavenRepositories", Vector.empty),
      carRepositories = lists.getOrElse("carRepositories", Vector.empty),
      sarRepositories = lists.getOrElse("sarRepositories", Vector.empty),
      coursierRepositories = lists.getOrElse("coursierRepositories", Vector.empty)
    )
  }

  private def _version_from_map(values: Map[String, String]): Option[RuntimeCatalogVersion] =
    values.get("version").filter(_.nonEmpty).map { version =>
      RuntimeCatalogVersion(
        version = version,
        channel = values.get("channel").filter(_.nonEmpty),
        status = values.get("status").filter(_.nonEmpty),
        scalaBinaryVersion = values.get("scalaBinaryVersion").filter(_.nonEmpty),
        module = values.get("module").filter(_.nonEmpty),
        publishedAt = values.get("publishedAt").filter(_.nonEmpty),
        checksumUrl = values.get("checksumUrl").filter(_.nonEmpty),
        metadataUrl = values.get("metadataUrl").filter(_.nonEmpty)
      )
    }

  private def _strip_comment(value: String): String = {
    val idx = value.indexOf('#')
    if (idx >= 0) value.substring(0, idx) else value
  }

  private def _split_key_value(value: String): (String, String) = {
    val idx = value.indexOf(':')
    (value.substring(0, idx).trim, value.substring(idx + 1).trim)
  }

  private def _unquote(value: String): String =
    value.stripPrefix("\"").stripSuffix("\"").stripPrefix("'").stripSuffix("'")
}

final class RuntimeCatalogStore(paths: LauncherPaths) {
  def loadCached(): Option[RuntimeCatalog] =
    if (Files.isRegularFile(paths.runtimeCatalog))
      Some(RuntimeCatalog.parse(Files.readString(paths.runtimeCatalog, StandardCharsets.UTF_8)))
    else
      None

  def refresh(config: LauncherConfig): RuntimeCatalog = {
    val url = config.runtimeCatalogUrl.getOrElse(LauncherConfig.DEFAULT_RUNTIME_CATALOG_URL)
    val text = _read_url_or_file(url)
    Files.createDirectories(paths.runtimeCatalog.getParent)
    Files.writeString(paths.runtimeCatalog, text, StandardCharsets.UTF_8)
    RuntimeCatalog.parse(text)
  }

  def loadOrRefresh(config: LauncherConfig): Option[RuntimeCatalog] =
    _load_cached_safe().orElse {
      try Some(refresh(config))
      catch {
        case _: Throwable => None
      }
    }

  private def _load_cached_safe(): Option[RuntimeCatalog] =
    try loadCached()
    catch {
      case _: Throwable => None
    }

  private def _read_url_or_file(value: String): String =
    if (value.startsWith("http://") || value.startsWith("https://")) {
      val connection = URI.create(value).toURL.openConnection()
      connection.setConnectTimeout(3000)
      connection.setReadTimeout(7000)
      Using.resource(scala.io.Source.fromInputStream(connection.getInputStream, "UTF-8"))(_.mkString)
    } else {
      Files.readString(Path.of(value), StandardCharsets.UTF_8)
    }
}

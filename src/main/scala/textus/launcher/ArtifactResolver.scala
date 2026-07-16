package textus.launcher

import java.net.URI
import java.nio.file.{Files, Path, Paths}
import scala.util.Using

/*
 * @since   May. 17, 2026
 *  version May. 25, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final case class ResolvedArtifact(
  selector: ArtifactSelector,
  kind: ArtifactKind,
  runtimeRequirements: Vector[RuntimeRequirement] = Vector.empty
)

private final case class ArtifactResolution(
  artifact: Option[ResolvedArtifact],
  diagnostics: Vector[String] = Vector.empty
)

final class ArtifactResolver {
  def resolve(selector: ArtifactSelector, config: LauncherConfig): ResolvedArtifact =
    selector.kind match {
      case ArtifactKind.Car =>
        _resolve_artifact(selector, config.carRepositories, ".car") match {
          case ArtifactResolution(Some(resolved), _) => resolved.copy(selector = resolved.selector.copy(kind = ArtifactKind.Car), kind = ArtifactKind.Car)
          case ArtifactResolution(None, diagnostics) =>
            throw TextusException(_not_found_message("CAR", selector, diagnostics))
        }
      case ArtifactKind.Sar =>
        _resolve_artifact(selector, config.sarRepositories, ".sar") match {
          case ArtifactResolution(Some(resolved), _) => resolved.copy(selector = resolved.selector.copy(kind = ArtifactKind.Sar), kind = ArtifactKind.Sar)
          case ArtifactResolution(None, diagnostics) =>
            throw TextusException(_not_found_message("SAR", selector, diagnostics))
        }
      case ArtifactKind.Auto =>
        val carartifact = _resolve_artifact(selector, config.carRepositories, ".car")
        val sarartifact = _resolve_artifact(selector, config.sarRepositories, ".sar")
        (carartifact.artifact, sarartifact.artifact) match {
          case (Some(resolved), None) =>
            resolved.copy(selector = resolved.selector.copy(kind = ArtifactKind.Car), kind = ArtifactKind.Car)
          case (None, Some(resolved)) =>
            resolved.copy(selector = resolved.selector.copy(kind = ArtifactKind.Sar), kind = ArtifactKind.Sar)
          case (Some(_), Some(_)) =>
            throw TextusException(s"artifact is ambiguous between CAR and SAR: ${selector.display}; use --car or --sar")
          case (None, None) =>
            throw TextusException(_not_found_message("artifact", selector, carartifact.diagnostics ++ sarartifact.diagnostics))
        }
    }

  private def _resolve_artifact(
    selector: ArtifactSelector,
    repositories: Vector[String],
    suffix: String
  ): ArtifactResolution = {
    val iterator = _effective_repositories(selector, repositories).iterator
    var diagnostics = Vector.empty[String]
    var artifact: Option[ResolvedArtifact] = None
    while (iterator.hasNext && artifact.isEmpty) {
      val resolution = _resolve_artifact_in_repository(selector, iterator.next(), suffix)
      artifact = resolution.artifact
      diagnostics ++= resolution.diagnostics
    }
    ArtifactResolution(artifact, diagnostics)
  }

  private def _resolve_artifact_in_repository(
    selector: ArtifactSelector,
    repository: String,
    suffix: String
  ): ArtifactResolution =
    if (_is_url(repository))
      _remote_resolve_artifact(selector, repository, suffix)
    else
      _local_resolve_artifact(selector, Paths.get(repository), suffix)

  private def _local_resolve_artifact(
    selector: ArtifactSelector,
    repository: Path,
    suffix: String
  ): ArtifactResolution =
    _local_catalog_path(repository, selector.name, suffix) match {
      case Some(path) =>
        _local_catalog_artifact(selector, repository, suffix, path)
      case None =>
        ArtifactResolution(_local_metadata_artifact(selector, repository, suffix))
    }

  private def _local_metadata_artifact(
    selector: ArtifactSelector,
    repository: Path,
    suffix: String
  ): Option[ResolvedArtifact] = {
    val root = repository.resolve(selector.name)
    if (!Files.isDirectory(root)) {
      None
    } else {
      val versions = selector.version.map(Vector(_)).getOrElse(_local_versions(root))
      versions.find(v => Files.isRegularFile(root.resolve(v).resolve(s"${selector.name}-$v$suffix"))).
        map(v => ResolvedArtifact(selector.copy(version = Some(v)), _kind_from_suffix(suffix)))
    }
  }

  private def _remote_resolve_artifact(
    selector: ArtifactSelector,
    repository: String,
    suffix: String
  ): ArtifactResolution = {
    val catalogurl = _catalog_url(repository, selector.name, suffix)
    if (_head(catalogurl))
      _remote_catalog_artifact(selector, repository, suffix, catalogurl)
    else
      ArtifactResolution(_remote_metadata_artifact(selector, repository, suffix))
  }

  private def _remote_metadata_artifact(
    selector: ArtifactSelector,
    repository: String,
    suffix: String
  ): Option[ResolvedArtifact] = {
    val versions = selector.version.map(Vector(_)).getOrElse(_remote_versions(repository, selector.name))
    versions.find { v =>
      val url = _join(repository, selector.name, v, s"${selector.name}-$v$suffix")
      _head(url)
    }.map(v => ResolvedArtifact(selector.copy(version = Some(v)), _kind_from_suffix(suffix)))
  }

  private def _local_catalog_artifact(
    selector: ArtifactSelector,
    repository: Path,
    suffix: String,
    path: Path
  ): ArtifactResolution = {
    val catalog = RepositoryArtifactCatalog.parse(Files.readString(path))
    catalog.resolve(selector.version) match {
      case Some(version) =>
        _local_catalog_file_exists(repository, selector.name, suffix, version) match {
          case Some(_) =>
            val requirements = version.runtime.toVector.map(_.withSource(selector.name)) ++ _local_dependency_requirements(repository, catalog)
            ArtifactResolution(Some(ResolvedArtifact(selector.copy(version = Some(version.version)), _kind_from_suffix(suffix), requirements)))
          case None =>
            ArtifactResolution(None, Vector(s"local catalog points to a missing archive: ${selector.name}:${version.version}: $path"))
        }
      case None if selector.version.exists(version => !catalog.versions.exists(_.version == version)) =>
        ArtifactResolution(None, Vector(s"local catalog does not contain ${selector.display}: $path"))
      case None =>
        throw TextusException(s"artifact catalog does not contain an enabled version for ${selector.display}: $path")
    }
  }

  private def _remote_catalog_artifact(
    selector: ArtifactSelector,
    repository: String,
    suffix: String,
    catalogurl: String
  ): ArtifactResolution = {
    val catalog = RepositoryArtifactCatalog.parse(_remote_catalog_text_required(catalogurl))
    catalog.resolve(selector.version) match {
      case Some(version) =>
        _remote_catalog_file_exists(repository, selector.name, suffix, version) match {
          case Some(_) =>
            val requirements = version.runtime.toVector.map(_.withSource(selector.name)) ++ _remote_dependency_requirements(repository, catalog)
            ArtifactResolution(Some(ResolvedArtifact(selector.copy(version = Some(version.version)), _kind_from_suffix(suffix), requirements)))
          case None =>
            ArtifactResolution(None, Vector(s"remote catalog points to a missing archive: ${selector.name}:${version.version}: $catalogurl"))
        }
      case None if selector.version.exists(version => !catalog.versions.exists(_.version == version)) =>
        ArtifactResolution(None, Vector(s"remote catalog does not contain ${selector.display}: $catalogurl"))
      case None =>
        throw TextusException(s"artifact catalog does not contain an enabled version for ${selector.display}: $catalogurl")
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

  private def _effective_repositories(
    selector: ArtifactSelector,
    repositories: Vector[String]
  ): Vector[String] =
    if (selector.version.exists(_is_snapshot_version))
      repositories.filterNot(_is_url).filterNot(_is_cache_repository)
    else
      repositories

  private def _is_snapshot_version(version: String): Boolean =
    version.toUpperCase(java.util.Locale.ROOT).endsWith("-SNAPSHOT")

  private def _is_cache_repository(repository: String): Boolean =
    repository.contains("/.cncf/cache/")

  private def _not_found_message(
    kind: String,
    selector: ArtifactSelector,
    diagnostics: Vector[String] = Vector.empty
  ): String = {
    val message = selector.version match {
      case Some(version) if _is_snapshot_version(version) =>
        s"snapshot component not found locally: ${selector.display}; run sbt cozyPublishLocalCar"
      case _ =>
        if (kind == "artifact")
          s"artifact not found in CAR/SAR repositories: ${selector.display}"
        else
          s"${kind} artifact not found in repositories: ${selector.display}"
    }
    if (diagnostics.isEmpty) message else s"$message; ${diagnostics.mkString("; ")}"
  }

  private def _kind_from_suffix(suffix: String): ArtifactKind =
    if (suffix == ".sar") ArtifactKind.Sar else ArtifactKind.Car

  private def _local_catalog_path(
    repository: Path,
    name: String,
    suffix: String
  ): Option[Path] = {
    val kind = suffix.drop(1)
    Option(repository.getParent).map(_.resolve("catalog").resolve(kind).resolve(s"$name.yaml")).filter(Files.isRegularFile(_))
  }

  private def _local_catalog_file_exists(
    repository: Path,
    name: String,
    suffix: String,
    version: RepositoryArtifactCatalogVersion
  ): Option[Unit] = {
    val default = repository.resolve(name).resolve(version.version).resolve(s"$name-${version.version}$suffix")
    val path = version.file.map(f => repository.getParent.resolve(f.stripPrefix("repository/"))).getOrElse(default)
    Option.when(Files.isRegularFile(path))(())
  }

  private def _remote_catalog_text(
    repository: String,
    name: String,
    suffix: String
  ): Option[String] = {
    val url = _catalog_url(repository, name, suffix)
    try {
      val connection = URI.create(url).toURL.openConnection()
      connection.setConnectTimeout(2000)
      connection.setReadTimeout(5000)
      Some(Using.resource(scala.io.Source.fromInputStream(connection.getInputStream, "UTF-8"))(_.mkString))
    } catch {
      case _: Throwable => None
    }
  }

  private def _remote_catalog_text_required(url: String): String =
    try {
      val connection = URI.create(url).toURL.openConnection()
      connection.setConnectTimeout(2000)
      connection.setReadTimeout(5000)
      Using.resource(scala.io.Source.fromInputStream(connection.getInputStream, "UTF-8"))(_.mkString)
    } catch {
      case e: Throwable => throw TextusException(s"failed to read artifact catalog: $url: ${e.getMessage}")
    }

  private def _remote_catalog_file_exists(
    repository: String,
    name: String,
    suffix: String,
    version: RepositoryArtifactCatalogVersion
  ): Option[Unit] = {
    val default = _join(repository, name, version.version, s"$name-${version.version}$suffix")
    val url = version.file.map(f => _repository_root_url(repository) + "/" + f.stripPrefix("repository/")).getOrElse(default)
    Option.when(_head(url))(())
  }

  private def _local_dependency_requirements(
    repository: Path,
    catalog: RepositoryArtifactCatalog
  ): Vector[RuntimeRequirement] =
    catalog.dependencies.flatMap { dependency =>
      val suffix = dependency.suffix
      _local_catalog_path(repository, dependency.selector.name, suffix).toVector.flatMap { path =>
        val depcatalog = RepositoryArtifactCatalog.parse(Files.readString(path))
        depcatalog.resolve(dependency.selector.version).toVector.flatMap(_.runtime.map(_.withSource(dependency.selector.name)))
      }
    }

  private def _remote_dependency_requirements(
    repository: String,
    catalog: RepositoryArtifactCatalog
  ): Vector[RuntimeRequirement] =
    catalog.dependencies.flatMap { dependency =>
      val suffix = dependency.suffix
      _remote_catalog_text(repository, dependency.selector.name, suffix).toVector.flatMap { text =>
        val depcatalog = RepositoryArtifactCatalog.parse(text)
        depcatalog.resolve(dependency.selector.version).toVector.flatMap(_.runtime.map(_.withSource(dependency.selector.name)))
      }
    }

  private def _catalog_url(
    repository: String,
    name: String,
    suffix: String
  ): String =
    _join(_repository_root_url(repository), "catalog", suffix.drop(1), s"$name.yaml")

  private def _repository_root_url(repository: String): String =
    {
      val clean = repository.reverse.dropWhile(_ == '/').reverse
      clean.substring(0, clean.lastIndexOf('/'))
    }

  private def _join(parts: String*): String =
    parts.toVector.zipWithIndex.map { case (p, idx) =>
      if (idx == 0) p.reverse.dropWhile(_ == '/').reverse
      else p.dropWhile(_ == '/').reverse.dropWhile(_ == '/').reverse
    }.mkString("/")

  private def _first_tag(text: String, tag: String): Option[String] =
    s"<$tag>([^<]+)</$tag>".r.findFirstMatchIn(text).map(_.group(1).trim).filter(_.nonEmpty)
}

final case class RepositoryArtifactCatalog(
  recommended: Option[String],
  latestStable: Option[String],
  latestSnapshot: Option[String],
  dependencies: Vector[RepositoryArtifactDependency],
  versions: Vector[RepositoryArtifactCatalogVersion]
) {
  def resolve(selector: Option[String]): Option[RepositoryArtifactCatalogVersion] = {
    val version = selector.orElse(recommended).orElse(latestStable).orElse(latestSnapshot)
    version.flatMap(v => versions.find(x => x.version == v && x.status.forall(_ != "disabled"))).
      orElse(versions.filter(_.status.forall(_ != "disabled")).sortWith((a, b) => RuntimeVersionOrdering.compare(a.version, b.version) < 0).lastOption)
  }
}

final case class RepositoryArtifactCatalogVersion(
  version: String,
  status: Option[String],
  file: Option[String],
  runtime: Option[RuntimeRequirement]
)

final case class RepositoryArtifactDependency(
  selector: ArtifactSelector,
  kind: ArtifactKind
) {
  def suffix: String =
    kind match {
      case ArtifactKind.Sar => ".sar"
      case _ => ".car"
    }
}

object RepositoryArtifactCatalog {
  def parse(text: String): RepositoryArtifactCatalog = {
    val parsed = SimpleYaml.parse(text)
    val versions = _version_maps(text).flatMap(_version)
    RepositoryArtifactCatalog(
      recommended = _first(parsed, "recommended"),
      latestStable = _first(parsed, "latestStable"),
      latestSnapshot = _first(parsed, "latestSnapshot"),
      dependencies = _dependencies(parsed),
      versions = versions
    )
  }

  private def _version(values: Map[String, Vector[String]]): Option[RepositoryArtifactCatalogVersion] =
    _first(values, "version").map { version =>
      val runtime = RuntimeRequirement(
        minimum = _first(values, "runtime.cncf.minimum"),
        maximum = _first(values, "runtime.cncf.maximum"),
        excluded = _list(values, "runtime.cncf.excluded"),
        tested = _list(values, "runtime.cncf.tested")
      )
      RepositoryArtifactCatalogVersion(
        version = version,
        status = _first(values, "status"),
        file = _first(values, "file"),
        runtime = Option.when(!runtime.isEmpty)(runtime)
      )
    }

  private def _version_maps(text: String): Vector[Map[String, Vector[String]]] = {
    var versions = Vector.empty[Map[String, Vector[String]]]
    var versionvalues = Map.empty[String, Vector[String]]
    var versionstack = Vector.empty[(Int, String)]
    var inversions = false

    def _finish_version_(): Unit =
      if (versionvalues.nonEmpty) {
        versions :+= versionvalues
        versionvalues = Map.empty
      }

    def _drop_stack_(stack: Vector[(Int, String)], indent: Int): Vector[(Int, String)] =
      stack.dropRight(stack.reverse.takeWhile(_._1 >= indent).length)

    def _path_(stack: Vector[(Int, String)], key: String): String =
      (stack.map(_._2) :+ key).mkString(".")

    def _append_(path: String, value: String): Unit =
      if (path.nonEmpty)
        versionvalues = versionvalues.updated(path, versionvalues.getOrElse(path, Vector.empty) :+ _unquote(value))

    text.linesIterator.foreach { raw =>
      val line = _strip_comment(raw)
      if (line.trim.nonEmpty) {
        val indent = line.takeWhile(_ == ' ').length
        val trimmed = line.trim
        if (indent == 0)
          inversions = trimmed == "versions:"
        if (inversions && indent == 2 && trimmed.startsWith("- ")) {
          _finish_version_()
          versionstack = Vector.empty
          val rest = trimmed.drop(2).trim
          if (rest.contains(":")) {
            val (key, value) = _split_key_value(rest)
            _append_(key, value)
          }
        } else if (inversions && indent > 2) {
          if (trimmed.startsWith("- ")) {
            versionstack = _drop_stack_(versionstack, indent)
            _append_(versionstack.map(_._2).mkString("."), trimmed.drop(2).trim)
          } else if (trimmed.contains(":")) {
            val (key, value) = _split_key_value(trimmed)
            versionstack = _drop_stack_(versionstack, indent)
            if (value.isEmpty)
              versionstack :+= indent -> key
            else
              _append_(_path_(versionstack, key), value)
          }
        }
      }
    }
    _finish_version_()
    versions
  }

  private def _first(values: Map[String, Vector[String]], key: String): Option[String] =
    values.getOrElse(key, Vector.empty).headOption.map(_.trim).filter(_.nonEmpty)

  private def _list(values: Map[String, Vector[String]], key: String): Vector[String] =
    values.getOrElse(key, Vector.empty).flatMap { value =>
      val clean = value.trim
      if (clean.isEmpty || clean == "[]")
        Vector.empty
      else if (clean.startsWith("[") && clean.endsWith("]"))
        clean.stripPrefix("[").stripSuffix("]").split(",").toVector.map(_unquote).map(_.trim).filter(_.nonEmpty)
      else
        Vector(_unquote(clean))
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

  private def _dependencies(values: Map[String, Vector[String]]): Vector[RepositoryArtifactDependency] =
    values.getOrElse("dependencies.car", Vector.empty).map { value =>
      RepositoryArtifactDependency(TextusCommandParser.parseArtifact(value, ArtifactKind.Car), ArtifactKind.Car)
    } ++ values.getOrElse("dependencies.sar", Vector.empty).map { value =>
      RepositoryArtifactDependency(TextusCommandParser.parseArtifact(value, ArtifactKind.Sar), ArtifactKind.Sar)
    }
}

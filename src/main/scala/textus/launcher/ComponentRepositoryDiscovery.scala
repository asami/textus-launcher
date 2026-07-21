package textus.launcher

import java.net.{HttpURLConnection, URI}
import java.nio.charset.StandardCharsets
import java.nio.file.{AtomicMoveNotSupportedException, Files, Path, StandardCopyOption}
import java.security.MessageDigest
import java.time.Instant
import scala.util.Try
import io.circe.{ACursor, Decoder, HCursor, Json}
import io.circe.parser

/*
 * Network-bounded consumer for the CNCF Component Repository index contract.
 * Listing is deliberately cache/local-only; only refresh and selected detail
 * lookup may contact a configured repository root.
 *
 * @since   Jul. 21, 2026
 * @version Jul. 21, 2026
 * @author  ASAMI, Tomoharu
 */
final case class ComponentRepositoryIndexEntry(
  kind: String,
  artifactId: String,
  catalog: String,
  status: String,
  recommended: Option[String],
  latestStable: Option[String],
  latestSnapshot: Option[String]
) {
  def identity: (String, String) = kind -> artifactId
}

final case class ComponentRepositoryIndex(
  schemaVersion: String,
  generatedAt: Instant,
  artifacts: Vector[ComponentRepositoryIndexEntry]
)

object ComponentRepositoryIndex {
  val SchemaVersion = "cncf.component-repository-index.v1"
  private val _valid_kinds = Set("car", "sar")
  private val _valid_statuses = Set("active", "deprecated", "disabled")
  private val _artifact_id_pattern = "[A-Za-z0-9][A-Za-z0-9._-]*".r

  def parse(text: String): ComponentRepositoryIndex = {
    val json = parser.parse(text).fold(error => throw TextusException(s"invalid component repository index JSON: ${error.message}"), identity)
    val cursor = json.hcursor
    _only_fields(cursor, Set("schemaVersion", "generatedAt", "artifacts"), "index")
    val schemaversion = _required[String](cursor, "schemaVersion", "index")
    if (schemaversion != SchemaVersion)
      throw TextusException(s"unsupported component repository index schemaVersion: $schemaversion")
    val generatedtext = _required[String](cursor, "generatedAt", "index")
    val generatedat = Try(Instant.parse(generatedtext)).getOrElse(throw TextusException(s"invalid component repository index generatedAt: $generatedtext"))
    val artifacts = _required[Vector[Json]](cursor, "artifacts", "index").zipWithIndex.map { case (value, index) =>
      _entry(value.hcursor, index)
    }.sortBy(_.identity)
    val duplicates = artifacts.groupBy(_.identity).collect { case (identity, values) if values.size > 1 => s"${identity._1}:${identity._2}" }.toVector.sorted
    if (duplicates.nonEmpty)
      throw TextusException(s"duplicate component repository artifacts: ${duplicates.mkString(", ")}")
    ComponentRepositoryIndex(schemaversion, generatedat, artifacts)
  }

  private def _entry(cursor: HCursor, index: Int): ComponentRepositoryIndexEntry = {
    val context = s"artifact[$index]"
    _only_fields(cursor, Set("kind", "artifactId", "catalog", "status", "recommended", "latestStable", "latestSnapshot"), context)
    val entry = ComponentRepositoryIndexEntry(
      _required[String](cursor, "kind", context),
      _required[String](cursor, "artifactId", context),
      _required[String](cursor, "catalog", context),
      _required[String](cursor, "status", context),
      _optional[String](cursor, "recommended", context),
      _optional[String](cursor, "latestStable", context),
      _optional[String](cursor, "latestSnapshot", context)
    )
    if (!_valid_kinds.contains(entry.kind)) throw TextusException(s"unsupported component artifact kind: ${entry.kind}")
    if (!_artifact_id_pattern.matches(entry.artifactId)) throw TextusException(s"invalid component repository artifactId: ${entry.artifactId}")
    if (!_valid_statuses.contains(entry.status)) throw TextusException(s"invalid component repository artifact status: ${entry.status}")
    val extension = Vector(".yaml", ".yml", ".json").find(entry.catalog.endsWith)
    val expected = extension.map(ext => s"${entry.kind}/${entry.artifactId}$ext")
    if (!expected.contains(entry.catalog)) throw TextusException(s"invalid component repository catalog path: ${entry.catalog}")
    Vector(entry.recommended, entry.latestStable, entry.latestSnapshot).flatten.foreach { value =>
      if (value.trim.isEmpty) throw TextusException(s"component repository selector must not be empty: ${entry.kind}:${entry.artifactId}")
    }
    entry
  }

  private def _required[A: Decoder](cursor: ACursor, field: String, context: String): A =
    cursor.get[A](field).fold(error => throw TextusException(s"component repository index $context requires $field: ${error.message}"), identity)

  private def _optional[A: Decoder](cursor: ACursor, field: String, context: String): Option[A] =
    cursor.get[Option[A]](field).fold(error => throw TextusException(s"invalid component repository index $context $field: ${error.message}"), identity)

  private def _only_fields(cursor: HCursor, expected: Set[String], context: String): Unit = {
    val unknown = cursor.keys.toVector.flatten.filterNot(expected).sorted
    if (unknown.nonEmpty) throw TextusException(s"unknown component repository index $context fields: ${unknown.mkString(", ")}")
  }
}

final case class DiscoveredComponentArtifact(entry: ComponentRepositoryIndexEntry, source: String, origin: String) {
  def render: String = {
    val selector = entry.recommended.orElse(entry.latestStable).orElse(entry.latestSnapshot).getOrElse("-")
    s"${entry.kind}\t${entry.artifactId}\t${entry.status}\t$selector\t$origin\t${ComponentRepositoryDiscovery.safeSource(source)}"
  }

  def renderDetailed: String =
    Vector(
      s"kind: ${entry.kind}",
      s"artifact-id: ${entry.artifactId}",
      s"status: ${entry.status}",
      s"recommended: ${entry.recommended.getOrElse("-")}",
      s"latest-stable: ${entry.latestStable.getOrElse("-")}",
      s"latest-snapshot: ${entry.latestSnapshot.getOrElse("-")}",
      s"catalog: ${entry.catalog}",
      s"source: ${ComponentRepositoryDiscovery.safeSource(source)}",
      s"origin: $origin"
    ).mkString("\n")
}

final case class ComponentRepositoryListResult(artifacts: Vector[DiscoveredComponentArtifact], diagnostics: Vector[String])
final case class ComponentRepositoryShowResult(artifact: DiscoveredComponentArtifact, diagnostics: Vector[String])
final case class ComponentRepositoryRefreshResult(refreshed: Vector[String], failures: Vector[String], diagnostics: Vector[String])

trait ComponentRepositoryHttpClient {
  def get(url: String): String
}

object ComponentRepositoryHttpClient {
  object System extends ComponentRepositoryHttpClient {
    def get(url: String): String = {
      val connection = URI.create(url).toURL.openConnection().asInstanceOf[HttpURLConnection]
      connection.setConnectTimeout(5000)
      connection.setReadTimeout(10000)
      connection.setRequestMethod("GET")
      val status = connection.getResponseCode
      if (status / 100 != 2) {
        connection.disconnect()
        throw TextusException(s"component repository request failed ($status): ${ComponentRepositoryDiscovery.safeSource(url)}")
      }
      val stream = connection.getInputStream
      try {
        val bytes = stream.readNBytes(4 * 1024 * 1024 + 1)
        if (bytes.length > 4 * 1024 * 1024) throw TextusException("component repository response exceeds 4 MiB")
        new String(bytes, StandardCharsets.UTF_8)
      } finally {
        stream.close()
        connection.disconnect()
      }
    }
  }
}

final class ComponentRepositoryDiscovery(
  paths: LauncherPaths,
  httpClient: ComponentRepositoryHttpClient = ComponentRepositoryHttpClient.System,
  now: () => Instant = () => Instant.now()
) {
  import ComponentRepositoryDiscovery.*

  def list(config: LauncherConfig, kind: Option[ArtifactKind], source: Option[String]): ComponentRepositoryListResult = {
    val sources = _selected_sources(config, source)
    val attempts = sources.map(_load_source)
    val loaded = attempts.flatMap(_._1)
    val diagnostics = attempts.flatMap(_._2)
    val candidates = loaded.flatMap { case (repository, origin) =>
      repository.index.artifacts.map(DiscoveredComponentArtifact(_, repository.source, origin))
    }.filter(artifact => _kind_name(kind).forall(_ == artifact.entry.kind))
    val selected = candidates.groupBy(_.entry.identity).toVector.sortBy(_._1).map { case (identity, values) =>
      val priority = values.map(_origin_priority).min
      val samepriority = values.filter(_origin_priority(_) == priority).sortBy(_.source)
      val conflict = samepriority.map(_.source).distinct
      val warning = Option.when(conflict.size > 1)(s"conflicting ${identity._1}:${identity._2} entries at equal precedence; selected ${safeSource(samepriority.head.source)}")
      samepriority.head -> warning
    }
    ComponentRepositoryListResult(selected.map(_._1), diagnostics ++ selected.flatMap(_._2))
  }

  def show(config: LauncherConfig, artifactid: String, kind: Option[ArtifactKind], source: Option[String]): ComponentRepositoryShowResult = {
    val listed = list(config, kind, source)
    val matches = listed.artifacts.filter(_.entry.artifactId == artifactid)
    val selected = matches match {
      case Vector(value) => value
      case Vector() => throw TextusException(s"component repository artifact not found: $artifactid")
      case _ => throw TextusException(s"component repository artifact is ambiguous; specify --kind: $artifactid")
    }
    val detaildiagnostic = _validate_detail(selected)
    ComponentRepositoryShowResult(selected, listed.diagnostics ++ detaildiagnostic)
  }

  def refresh(config: LauncherConfig, source: Option[String]): ComponentRepositoryRefreshResult = {
    val sources = _selected_sources(config, source).filter(_.remote)
    if (source.isDefined && sources.isEmpty) throw TextusException(s"component repository source is not a configured remote root: ${safeSource(source.get)}")
    val results = sources.map { repository =>
      val attemptedat = now()
      try {
        val text = httpClient.get(_join(repository.source, "catalog/index.json"))
        val index = ComponentRepositoryIndex.parse(text)
        _write_cache(repository.source, text, index, attemptedat)
        Right(safeSource(repository.source))
      } catch {
        case e: Exception =>
          val message = _safe_error_message(e, repository.source)
          _write_failure(repository.source, attemptedat, message)
          Left(s"${safeSource(repository.source)}: $message")
      }
    }
    ComponentRepositoryRefreshResult(results.collect { case Right(value) => value }, results.collect { case Left(value) => value }, Vector.empty)
  }

  private def _load_source(source: RepositorySource): (Option[(LoadedRepositoryIndex, String)], Vector[String]) =
    if (source.remote) {
      val indexpath = _cache_dir(source.source).resolve("index.json")
      if (Files.isRegularFile(indexpath)) {
        Try(ComponentRepositoryIndex.parse(Files.readString(indexpath, StandardCharsets.UTF_8))).toEither match {
          case Right(index) => (Some(LoadedRepositoryIndex(source.source, index) -> "cache"), _cached_diagnostic(source.source).toVector)
          case Left(error) => (None, Vector(s"ignored malformed cached component repository index for ${safeSource(source.source)}: ${error.getMessage}"))
        }
      } else (None, Vector.empty)
    } else {
      val indexpath = Path.of(source.source).resolve("catalog/index.json")
      if (Files.isRegularFile(indexpath)) {
        Try(ComponentRepositoryIndex.parse(Files.readString(indexpath, StandardCharsets.UTF_8))).toEither match {
          case Right(index) => (Some(LoadedRepositoryIndex(source.source, index) -> "local"), Vector.empty)
          case Left(error) => (None, Vector(s"ignored malformed local component repository index for ${safeSource(source.source)}: ${error.getMessage}"))
        }
      } else (None, Vector.empty)
    }

  private def _validate_detail(artifact: DiscoveredComponentArtifact): Option[String] = {
    val (text, diagnostic) =
      if (_is_remote(artifact.source)) {
        val cache = _cache_dir(artifact.source).resolve("catalog").resolve(artifact.entry.catalog)
        try {
          val current = httpClient.get(_join(artifact.source, s"catalog/${artifact.entry.catalog}"))
          _validate_detail_text(artifact, current)
          _write_atomic(cache, current)
          current -> None
        } catch {
          case e: Exception if Files.isRegularFile(cache) =>
            Files.readString(cache, StandardCharsets.UTF_8) -> Some(
              s"using stale detailed catalog cache for ${artifact.entry.kind}:${artifact.entry.artifactId} after refresh failure: ${_safe_error_message(e, artifact.source)}"
            )
        }
      } else {
        val path = Path.of(artifact.source).resolve("catalog").resolve(artifact.entry.catalog).normalize()
        val root = Path.of(artifact.source).resolve("catalog").normalize()
        if (!path.startsWith(root) || !Files.isRegularFile(path)) throw TextusException(s"component repository catalog is missing: ${artifact.entry.catalog}")
        Files.readString(path, StandardCharsets.UTF_8) -> None
      }
    _validate_detail_text(artifact, text)
    diagnostic
  }

  private def _validate_detail_text(artifact: DiscoveredComponentArtifact, text: String): Unit = {
    def _first_(key: String): Option[String] =
      if (artifact.entry.catalog.endsWith(".json"))
        parser.parse(text).toOption.flatMap(_.hcursor.get[Option[String]](key).toOption.flatten)
      else
        SimpleYaml.parse(text).getOrElse(key, Vector.empty).headOption
    if (!_first_("kind").contains(artifact.entry.kind) || !_first_("artifactId").contains(artifact.entry.artifactId))
      throw TextusException(s"component repository catalog identity mismatch: ${artifact.entry.kind}:${artifact.entry.artifactId}")
    if (_first_("status").getOrElse("active") != artifact.entry.status)
      throw TextusException(s"component repository catalog status is stale: ${artifact.entry.kind}:${artifact.entry.artifactId}")
    Vector("recommended" -> artifact.entry.recommended, "latestStable" -> artifact.entry.latestStable, "latestSnapshot" -> artifact.entry.latestSnapshot).foreach {
      case (name, expected) if _first_(name) != expected => throw TextusException(s"component repository catalog $name selector is stale: ${artifact.entry.kind}:${artifact.entry.artifactId}")
      case _ => ()
    }
  }

  private def _selected_sources(config: LauncherConfig, requested: Option[String]): Vector[RepositorySource] = {
    val sources = (config.carRepositories ++ config.sarRepositories).flatMap(_repository_root).distinct.map(source => RepositorySource(source, _is_remote(source)))
    requested match {
      case None => sources
      case Some(value) =>
        val normalized = _repository_root(value).getOrElse(value.stripSuffix("/"))
        val selected = sources.filter(_.source == normalized)
        if (selected.isEmpty) throw TextusException(s"component repository source is not configured: ${safeSource(value)}")
        selected
    }
  }

  private def _write_cache(source: String, text: String, index: ComponentRepositoryIndex, timestamp: Instant): Unit = {
    val dir = _cache_dir(source)
    _write_atomic(dir.resolve("index.json"), text)
    _write_metadata(dir.resolve("metadata.json"), source, Some(timestamp), timestamp, index.schemaVersion, None)
  }

  private def _write_failure(source: String, timestamp: Instant, message: String): Unit = {
    val dir = _cache_dir(source)
    val retrievedat = _read_metadata(dir.resolve("metadata.json")).flatMap(_._1)
    val schema =
      if (Files.isRegularFile(dir.resolve("index.json"))) Try(ComponentRepositoryIndex.parse(Files.readString(dir.resolve("index.json"))).schemaVersion).getOrElse(ComponentRepositoryIndex.SchemaVersion)
      else ComponentRepositoryIndex.SchemaVersion
    _write_metadata(dir.resolve("metadata.json"), source, retrievedat, timestamp, schema, Some(message))
  }

  private def _write_metadata(path: Path, source: String, retrievedat: Option[Instant], lastattemptat: Instant, schema: String, error: Option[String]): Unit = {
    val fields: Vector[Option[(String, Json)]] = Vector(
      Some("source" -> Json.fromString(safeSource(source))),
      retrievedat.map(value => "retrievedAt" -> Json.fromString(value.toString)),
      Some("lastAttemptAt" -> Json.fromString(lastattemptat.toString)),
      Some("schemaVersion" -> Json.fromString(schema)),
      error.map(value => "lastError" -> Json.fromString(value))
    )
    _write_atomic(path, Json.obj(fields.flatten: _*).spaces2 + "\n")
  }

  private def _read_metadata(path: Path): Option[(Option[Instant], Option[String])] =
    Option.when(Files.isRegularFile(path))(Files.readString(path)).flatMap(text => parser.parse(text).toOption).map { json =>
      val cursor = json.hcursor
      cursor.get[String]("retrievedAt").toOption.flatMap(value => Try(Instant.parse(value)).toOption) -> cursor.get[String]("schemaVersion").toOption
    }

  private def _cached_diagnostic(source: String): Option[String] = {
    val path = _cache_dir(source).resolve("metadata.json")
    if (!Files.isRegularFile(path)) None
    else parser.parse(Files.readString(path)).toOption.flatMap(_.hcursor.get[String]("lastError").toOption).map(error => s"using stale component repository cache for ${safeSource(source)} after refresh failure: $error")
  }

  private def _cache_dir(source: String): Path = paths.componentRepositoryCache.resolve(_sha256(source))

  private def _safe_error_message(error: Exception, source: String): String =
    Option(error.getMessage).filter(_.nonEmpty).getOrElse(error.getClass.getSimpleName).replace(source, safeSource(source))

  private def _write_atomic(path: Path, text: String): Unit = {
    Files.createDirectories(path.getParent)
    val temporary = Files.createTempFile(path.getParent, ".component-repository-", ".tmp")
    try {
      Files.writeString(temporary, text, StandardCharsets.UTF_8)
      try Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
      catch { case _: AtomicMoveNotSupportedException => Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING) }
    } finally Files.deleteIfExists(temporary)
  }
}

object ComponentRepositoryDiscovery {
  private final case class RepositorySource(source: String, remote: Boolean)
  private final case class LoadedRepositoryIndex(source: String, index: ComponentRepositoryIndex)

  def apply(paths: LauncherPaths): ComponentRepositoryDiscovery = new ComponentRepositoryDiscovery(paths)

  def safeSource(value: String): String =
    Try {
      val uri = URI.create(value)
      if (uri.getScheme == null) s"local:${Path.of(value).getFileName}#${_sha256(value).take(8)}"
      else new URI(uri.getScheme, null, uri.getHost, uri.getPort, uri.getPath, null, null).toString
    }.getOrElse(s"source#${_sha256(value).take(8)}")

  private def _repository_root(repository: String): Option[String] = {
    val clean = repository.trim.stripSuffix("/")
    if (clean.isEmpty || clean.endsWith("/.cncf/cache/car") || clean.endsWith("/.cncf/cache/sar")) None
    else {
      val root = if (clean.endsWith("/car") || clean.endsWith("/sar")) clean.dropRight(4) else clean
      Some(if (root.startsWith("file:")) Path.of(URI.create(root)).normalize.toString else root)
    }
  }

  private def _kind_name(kind: Option[ArtifactKind]): Option[String] = kind.map {
    case ArtifactKind.Car => "car"
    case ArtifactKind.Sar => "sar"
    case ArtifactKind.Auto => throw TextusException("repository kind must be car or sar")
  }

  private def _origin_priority(value: DiscoveredComponentArtifact): Int = if (value.origin == "local") 0 else 1
  private def _is_remote(value: String): Boolean = value.startsWith("http://") || value.startsWith("https://")
  private def _join(root: String, path: String): String = s"${root.stripSuffix("/")}/${path.stripPrefix("/")}"
  private def _sha256(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)).map("%02x".format(_)).mkString
}

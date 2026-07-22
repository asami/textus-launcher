package textus.launcher

/*
 * @since   May. 17, 2026
 *  version May. 27, 2026
 *  version Jun. 29, 2026
 * @version Jul. 22, 2026
 * @author  ASAMI, Tomoharu
 */
enum ArtifactKind {
  case Car, Sar, Auto
}

final case class ArtifactSelector(
  name: String,
  version: Option[String] = None,
  kind: ArtifactKind = ArtifactKind.Auto
) {
  def display: String =
    version.map(v => s"$name:$v").getOrElse(name)
}

sealed trait TextusCommand

object TextusCommand {
  final case class InstallCli(
    name: String,
    artifact: ArtifactSelector,
    operationPrefix: String,
    binDir: Option[String],
    fileParams: Vector[String],
    overwrite: Boolean,
    runtimeVersion: Option[String] = None,
    runtimeDevDir: Option[String] = None,
    runtimeSelectionPolicy: Option[RuntimeSelectionPolicy] = None,
    runtimeNoCompatiblePolicy: Option[RuntimeNoCompatiblePolicy] = None
  ) extends TextusCommand

  final case class Execute(
    mode: String,
    artifact: ArtifactSelector,
    args: Vector[String],
    runtimeVersion: Option[String],
    runtimeDevDir: Option[String],
    runtimeSelectionPolicy: Option[RuntimeSelectionPolicy],
    runtimeNoCompatiblePolicy: Option[RuntimeNoCompatiblePolicy],
    passthrough: Vector[String]
  ) extends TextusCommand

  sealed trait Runtime extends TextusCommand
  object Runtime {
    final case class Version(runtimeVersion: Option[String], runtimeDevDir: Option[String]) extends Runtime
    case object Current extends Runtime
    case object LocalList extends Runtime
    case object RemoteList extends Runtime
    case object Refresh extends Runtime
    case object CatalogShow extends Runtime
    case object Channels extends Runtime
    final case class Install(version: String) extends Runtime
    final case class Use(version: String, target: RuntimeUseTarget) extends Runtime
    final case class CacheStatus() extends Runtime
    final case class ConfigShow() extends Runtime
  }

  sealed trait Repository extends TextusCommand
  object Repository {
    final case class ListArtifacts(kind: Option[ArtifactKind], source: Option[String]) extends Repository
    final case class Show(artifactId: String, kind: Option[ArtifactKind], source: Option[String]) extends Repository
    final case class Refresh(source: Option[String]) extends Repository
  }

  enum RuntimeUseTarget {
    case Auto, Global, Project
  }

  case object LauncherVersion extends TextusCommand
  case object RuntimeHelp extends TextusCommand
  case object LauncherHelp extends TextusCommand
}

object TextusCommandParser {
  def parse(args: Vector[String]): TextusCommand = {
    if (args == Vector("--version") || args == Vector("version")) {
      TextusCommand.Runtime.Version(None, None)
    } else if (args == Vector("launcher", "version") || args == Vector("launcher", "--version")) {
      TextusCommand.LauncherVersion
    } else if (args == Vector("help") || args == Vector("--help") || args == Vector("-h")) {
      TextusCommand.RuntimeHelp
    } else if (args.isEmpty || args == Vector("launcher", "help") || args == Vector("launcher", "--help")) {
      TextusCommand.LauncherHelp
    } else {
      val (runtimeversion, runtimedevdir, selectionpolicy, nocompatiblepolicy, rest) = _take_global_runtime(args)
      rest match {
        case Vector("--version") | Vector("version") =>
          TextusCommand.Runtime.Version(runtimeversion, runtimedevdir)
        case _ =>
          rest.headOption match {
            case Some("install-cli") =>
              _parse_install_cli(rest.tail, runtimeversion, runtimedevdir, selectionpolicy, nocompatiblepolicy)
            case Some("server") | Some("client") | Some("command") =>
              _parse_execute(rest, runtimeversion, runtimedevdir, selectionpolicy, nocompatiblepolicy)
            case Some("runtime") =>
              _parse_runtime(rest.tail)
            case Some("repository") =>
              _parse_repository(rest.tail)
            case Some(target) if rest.drop(1).headOption.exists(_is_target_mode) && !_is_reserved_target(target) =>
              _parse_target_execute(rest, runtimeversion, runtimedevdir, selectionpolicy, nocompatiblepolicy)
            case Some(other) =>
              throw TextusException(s"unknown textus command: $other")
            case None =>
              TextusCommand.LauncherHelp
          }
      }
    }
  }

  private def _parse_install_cli(
    args: Vector[String],
    runtimeversion: Option[String],
    runtimedevdir: Option[String],
    selectionpolicy: Option[RuntimeSelectionPolicy],
    nocompatiblepolicy: Option[RuntimeNoCompatiblePolicy]
  ): TextusCommand.InstallCli = {
    var name: Option[String] = None
    var artifact: Option[ArtifactSelector] = None
    var operationprefix: Option[String] = None
    var bindir: Option[String] = None
    var fileparams = Vector.empty[String]
    var overwrite = false
    var i = 0
    while (i < args.length) {
      args(i) match {
        case "--artifact" =>
          if (i + 1 >= args.length) throw TextusException("--artifact requires a value")
          artifact = Some(parseArtifact(args(i + 1)))
          i += 2
        case x if x.startsWith("--artifact=") =>
          artifact = Some(parseArtifact(x.stripPrefix("--artifact=")))
          i += 1
        case "--operation-prefix" =>
          if (i + 1 >= args.length) throw TextusException("--operation-prefix requires a value")
          operationprefix = Some(args(i + 1))
          i += 2
        case x if x.startsWith("--operation-prefix=") =>
          operationprefix = Some(x.stripPrefix("--operation-prefix="))
          i += 1
        case "--bin-dir" =>
          if (i + 1 >= args.length) throw TextusException("--bin-dir requires a value")
          bindir = Some(args(i + 1))
          i += 2
        case x if x.startsWith("--bin-dir=") =>
          bindir = Some(x.stripPrefix("--bin-dir="))
          i += 1
        case "--file-param" =>
          if (i + 1 >= args.length) throw TextusException("--file-param requires a value")
          fileparams :+= args(i + 1)
          i += 2
        case x if x.startsWith("--file-param=") =>
          fileparams :+= x.stripPrefix("--file-param=")
          i += 1
        case "--overwrite" =>
          overwrite = true
          i += 1
        case x if x.startsWith("--") =>
          throw TextusException(s"unknown textus install-cli option: $x")
        case x =>
          if (name.isEmpty) name = Some(x)
          else if (artifact.isEmpty) artifact = Some(parseArtifact(x))
          else throw TextusException(s"unexpected textus install-cli argument: $x")
          i += 1
      }
    }
    TextusCommand.InstallCli(
      name.getOrElse(throw TextusException("textus install-cli requires a command name")),
      artifact.getOrElse(throw TextusException("textus install-cli requires an artifact")),
      operationprefix.getOrElse(throw TextusException("textus install-cli requires --operation-prefix")),
      bindir,
      fileparams,
      overwrite,
      runtimeVersion = runtimeversion,
      runtimeDevDir = runtimedevdir,
      runtimeSelectionPolicy = selectionpolicy,
      runtimeNoCompatiblePolicy = nocompatiblepolicy
    )
  }

  private def _take_global_runtime(args: Vector[String]): (Option[String], Option[String], Option[RuntimeSelectionPolicy], Option[RuntimeNoCompatiblePolicy], Vector[String]) = {
    val out = Vector.newBuilder[String]
    var runtime: Option[String] = None
    var runtimedevdir: Option[String] = None
    var selectionpolicy: Option[RuntimeSelectionPolicy] = None
    var nocompatiblepolicy: Option[RuntimeNoCompatiblePolicy] = None
    var i = 0
    while (i < args.length) {
      args(i) match {
        case "--runtime" =>
          if (i + 1 >= args.length)
            throw TextusException("--runtime requires a value")
          runtime = Some(args(i + 1))
          i += 2
        case x if x.startsWith("--runtime=") =>
          runtime = Some(x.stripPrefix("--runtime="))
          i += 1
        case "--runtime-dev-dir" =>
          if (i + 1 >= args.length)
            throw TextusException("--runtime-dev-dir requires a value")
          runtimedevdir = Some(args(i + 1))
          i += 2
        case x if x.startsWith("--runtime-dev-dir=") =>
          runtimedevdir = Some(x.stripPrefix("--runtime-dev-dir="))
          i += 1
        case "--runtime-selection" =>
          if (i + 1 >= args.length)
            throw TextusException("--runtime-selection requires a value")
          selectionpolicy = Some(RuntimeSelectionPolicy.parse(args(i + 1)))
          i += 2
        case x if x.startsWith("--runtime-selection=") =>
          selectionpolicy = Some(RuntimeSelectionPolicy.parse(x.stripPrefix("--runtime-selection=")))
          i += 1
        case "--runtime-no-compatible" =>
          if (i + 1 >= args.length)
            throw TextusException("--runtime-no-compatible requires a value")
          nocompatiblepolicy = Some(RuntimeNoCompatiblePolicy.parse(args(i + 1)))
          i += 2
        case x if x.startsWith("--runtime-no-compatible=") =>
          nocompatiblepolicy = Some(RuntimeNoCompatiblePolicy.parse(x.stripPrefix("--runtime-no-compatible=")))
          i += 1
        case x =>
          out += x
          i += 1
      }
    }
    (runtime, runtimedevdir, selectionpolicy, nocompatiblepolicy, out.result())
  }

  private def _is_target_mode(value: String): Boolean =
    value == "command" || value == "server" || value == "client"

  private def _is_reserved_target(value: String): Boolean =
    Set("runtime", "repository", "install-cli", "version", "help", "launcher", "dev").contains(value)

  private def _parse_repository(args: Vector[String]): TextusCommand.Repository = {
    if (args.isEmpty)
      throw TextusException("textus repository requires list, show, or refresh")
    val operation = args.head
    var kind: Option[ArtifactKind] = None
    var source: Option[String] = None
    var positional = Vector.empty[String]
    var i = 1
    while (i < args.length) {
      args(i) match {
        case "--kind" =>
          if (i + 1 >= args.length) throw TextusException("--kind requires car or sar")
          kind = Some(_repository_kind(args(i + 1)))
          i += 2
        case value if value.startsWith("--kind=") =>
          kind = Some(_repository_kind(value.stripPrefix("--kind=")))
          i += 1
        case "--source" =>
          if (i + 1 >= args.length) throw TextusException("--source requires a configured repository root")
          source = Some(args(i + 1))
          i += 2
        case value if value.startsWith("--source=") =>
          source = Some(value.stripPrefix("--source="))
          i += 1
        case value if value.startsWith("--") =>
          throw TextusException(s"unknown textus repository option: $value")
        case value =>
          positional :+= value
          i += 1
      }
    }
    operation match {
      case "list" if positional.isEmpty => TextusCommand.Repository.ListArtifacts(kind, source)
      case "show" if positional.size == 1 => TextusCommand.Repository.Show(positional.head, kind, source)
      case "refresh" if positional.size <= 1 && kind.isEmpty => TextusCommand.Repository.Refresh(source.orElse(positional.headOption))
      case "list" => throw TextusException("textus repository list accepts only --kind and --source")
      case "show" => throw TextusException("textus repository show requires one artifact id")
      case "refresh" => throw TextusException("textus repository refresh accepts one configured source")
      case other => throw TextusException(s"unknown textus repository command: $other")
    }
  }

  private def _repository_kind(value: String): ArtifactKind =
    value.toLowerCase match {
      case "car" => ArtifactKind.Car
      case "sar" => ArtifactKind.Sar
      case other => throw TextusException(s"unsupported repository artifact kind: $other")
    }

  private def _parse_target_execute(
    args: Vector[String],
    runtimeversion: Option[String],
    runtimedevdir: Option[String],
    selectionpolicy: Option[RuntimeSelectionPolicy],
    nocompatiblepolicy: Option[RuntimeNoCompatiblePolicy]
  ): TextusCommand.Execute = {
    val target = args.head
    val mode = args(1)
    val rest0 = args.drop(2)
    val (pre, passthrough) = rest0.span(_ != "--")
    val effectivepassthrough = if (passthrough.isEmpty) Vector.empty else passthrough.tail
    TextusCommand.Execute(mode, parseArtifact(target), pre, runtimeversion, runtimedevdir, selectionpolicy, nocompatiblepolicy, effectivepassthrough)
  }

  private def _parse_execute(
    args: Vector[String],
    runtimeversion: Option[String],
    runtimedevdir: Option[String],
    selectionpolicy: Option[RuntimeSelectionPolicy],
    nocompatiblepolicy: Option[RuntimeNoCompatiblePolicy]
  ): TextusCommand.Execute = {
    val mode = args.head
    val rest0 = args.tail
    val (pre, passthrough) = rest0.span(_ != "--")
    val effectivepassthrough = if (passthrough.isEmpty) Vector.empty else passthrough.tail
    if (pre.isEmpty)
      throw TextusException(s"textus $mode requires a CAR or SAR artifact")
    val (kind, rest1) =
      pre.headOption match {
        case Some("--car") =>
          if (pre.length < 2) throw TextusException("--car requires an artifact name")
          (ArtifactKind.Car, pre.tail)
        case Some("--sar") =>
          if (pre.length < 2) throw TextusException("--sar requires an artifact name")
          (ArtifactKind.Sar, pre.tail)
        case _ =>
          (ArtifactKind.Auto, pre)
      }
    val artifact = parseArtifact(rest1.head, kind)
    TextusCommand.Execute(mode, artifact, rest1.tail, runtimeversion, runtimedevdir, selectionpolicy, nocompatiblepolicy, effectivepassthrough)
  }

  def parseArtifact(value: String, forcedkind: ArtifactKind = ArtifactKind.Auto): ArtifactSelector = {
    val colon = value.lastIndexOf(':')
    val at = value.lastIndexOf('@')
    if (colon > 0 && at > 0)
      throw TextusException(s"artifact version uses both ':' and '@': $value")
    val separator = if (colon > 0) colon else at
    val (rawname, version) =
      if (separator > 0 && separator + 1 < value.length)
        (value.substring(0, separator), Some(value.substring(separator + 1)))
      else
        (value, None)
    val inferred =
      if (rawname.endsWith(".car")) ArtifactKind.Car
      else if (rawname.endsWith(".sar")) ArtifactKind.Sar
      else forcedkind
    val withoutextension =
      if (rawname.endsWith(".car") || rawname.endsWith(".sar"))
        rawname.dropRight(4)
      else
        rawname
    ArtifactSelector(withoutextension, version, inferred)
  }

  private def _parse_runtime(args: Vector[String]): TextusCommand.Runtime =
    args match {
      case Vector("current") => TextusCommand.Runtime.Current
      case Vector("list") => TextusCommand.Runtime.LocalList
      case Vector("local", "list") => TextusCommand.Runtime.LocalList
      case Vector("remote", "list") => TextusCommand.Runtime.RemoteList
      case Vector("refresh") => TextusCommand.Runtime.Refresh
      case Vector("catalog", "show") => TextusCommand.Runtime.CatalogShow
      case Vector("channels") => TextusCommand.Runtime.Channels
      case Vector("install", version) => TextusCommand.Runtime.Install(version)
      case Vector("use", version) =>
        TextusCommand.Runtime.Use(version, TextusCommand.RuntimeUseTarget.Auto)
      case Vector("use", version, "--global") =>
        TextusCommand.Runtime.Use(version, TextusCommand.RuntimeUseTarget.Global)
      case Vector("use", version, "--project") =>
        TextusCommand.Runtime.Use(version, TextusCommand.RuntimeUseTarget.Project)
      case Vector("cache", "status") => TextusCommand.Runtime.CacheStatus()
      case Vector("config", "show") => TextusCommand.Runtime.ConfigShow()
      case other =>
        throw TextusException(s"unknown textus runtime command: ${other.mkString(" ")}")
    }

  val helpText: String =
    """Usage:
      |  textus --version
      |  textus version
      |  textus [--runtime <version>] [--runtime-dev-dir <dir>] version
      |  textus launcher version
      |  textus install-cli <command-name> <artifact> --operation-prefix <component.service> [--file-param <name>...] [--bin-dir <dir>] [--overwrite]
      |  textus [--runtime <version>] [--runtime-dev-dir <dir>] <target> server [options...]
      |  textus [--runtime <version>] [--runtime-dev-dir <dir>] <target> client [args...]
      |  textus [--runtime <version>] [--runtime-dev-dir <dir>] <target> command <operation> [params...]
      |  textus [--runtime <version>] [--runtime-dev-dir <dir>] server  <artifact>[:<version>] [options...]   (compatibility)
      |  textus [--runtime <version>] [--runtime-dev-dir <dir>] client  <artifact>[:<version>] [args...]       (compatibility)
      |  textus [--runtime <version>] [--runtime-dev-dir <dir>] command <artifact>[:<version>] <operation> [params...] (compatibility)
      |  textus runtime current
      |  textus runtime list
      |  textus runtime local list
      |  textus runtime remote list
      |  textus runtime refresh
      |  textus runtime catalog show
      |  textus runtime channels
      |  textus runtime install <version>
      |  textus runtime use <version>
      |  textus runtime use <version> --global
      |  textus runtime use <version> --project
      |  textus runtime cache status
      |  textus runtime config show
      |  textus repository list [--kind car|sar] [--source <configured-root>]
      |  textus repository show <artifact-id> [--kind car|sar] [--source <configured-root>]
      |  textus repository refresh [<configured-root>|--source <configured-root>]
      |
      |Artifact:
      |  textus-blog          auto-detect CAR/SAR from repositories
      |  textus-blog.car      force CAR
      |  textus-blog.sar      force SAR
      |  textus-blog:0.1.0    select artifact version
      |  --car textus-blog    force CAR
      |  --sar my-app         force SAR
      |  ~/.cncf/local/repository/car and ~/.cncf/local/repository/sar are searched before cache and public repositories.
      |  Use sbt cozyPublishLocalCar or sbt cozyPublishLocalSar while developing dependency components.
      |  ~/.cncf/local is developer local publish state; ~/.cncf/cache is runtime-managed remote artifact cache.
      |  Snapshot components are local-only by default; missing snapshots should be published with sbt cozyPublishLocalCar.
      |  Repository list uses local and cached component indexes without network or archive downloads.
      |  Repository refresh only contacts configured repository roots and preserves stale cache on failure.
      |
      |Compatibility:
      |  artifact@version remains accepted as a legacy spelling.
      |
      |Runtime:
      |  Config launcher.dev-dir delegates the installed launcher to a Textus launcher development checkout.
      |  Run sbt textusExportLauncherClasspath in that checkout before enabling launcher.dev-dir.
      |  --runtime <version> overrides .textus/version and ~/.textus/version.
      |  --runtime-dev-dir <dir> runs a CNCF runtime from a development checkout.
      |  Config runtime.dev-dir is the configuration equivalent of --runtime-dev-dir.
      |  development.runtime.dev-dir is activated by TEXTUS_USE_DEVELOPMENT=true.
      |  TEXTUS_RUNTIME_DEV_DIR or CNCF_RUNTIME_DEV_DIR overrides the active runtime development checkout.
      |  --launcher-home <dir> selects an explicit Launcher state-home root for isolated integration tests; omit it in normal operation, where textus <artifact> server uses ~/.cncf and ~/.textus.
      |  --config <file> loads an additional Textus launcher config file.
      |  Textus launcher config loads from ~/.textus/config.yaml and ~/.textus/launcher.yaml, then ancestor and cwd .textus config/launcher files.
      |  Textus launcher config supports yaml/yml, properties/props, and lightweight conf files.
      |  JSON, XML, and full HOCON are runtime/application config formats, not launcher config formats.
      |  Without --runtime, component runtime.cncf requirements use current-compatible selection by default.
      |  --runtime-selection=current-compatible|tested-latest|latest-compatible|newest-compatible selects the compatible runtime preference.
      |  --runtime-no-compatible=error|newest controls the fallback when no compatible runtime exists.
      |
      |Install CLI:
      |  textus install-cli creates a user-facing command for a packaged CAR/SAR artifact.
      |  The installed command runs textus <artifact> command <operation-prefix>.<operation>.
      |  --file-param <name> reads existing file values before passing operation parameters.
      |""".stripMargin
}

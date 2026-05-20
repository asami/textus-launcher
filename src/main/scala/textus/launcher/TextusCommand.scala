package textus.launcher

/*
 * @since   May. 17, 2026
 * @version May. 21, 2026
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
  final case class Execute(
    mode: String,
    artifact: ArtifactSelector,
    args: Vector[String],
    runtimeVersion: Option[String],
    runtimeSelectionPolicy: Option[RuntimeSelectionPolicy],
    runtimeNoCompatiblePolicy: Option[RuntimeNoCompatiblePolicy],
    passthrough: Vector[String]
  ) extends TextusCommand

  sealed trait Runtime extends TextusCommand
  object Runtime {
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

  enum RuntimeUseTarget {
    case Auto, Global, Project
  }

  case object Version extends TextusCommand
  case object Help extends TextusCommand
}

object TextusCommandParser {
  def parse(args: Vector[String]): TextusCommand = {
    if (args == Vector("--version") || args == Vector("version") || args == Vector("launcher", "version")) {
      TextusCommand.Version
    } else if (args.isEmpty || args.contains("--help") || args.contains("-h")) {
      TextusCommand.Help
    } else {
      val (runtimeversion, selectionpolicy, nocompatiblepolicy, rest) = _take_global_runtime(args)
      rest.headOption match {
        case Some("server") | Some("client") | Some("command") =>
          _parse_execute(rest, runtimeversion, selectionpolicy, nocompatiblepolicy)
        case Some("runtime") =>
          _parse_runtime(rest.tail)
        case Some(other) =>
          throw TextusException(s"unknown textus command: $other")
        case None =>
          TextusCommand.Help
      }
    }
  }

  private def _take_global_runtime(args: Vector[String]): (Option[String], Option[RuntimeSelectionPolicy], Option[RuntimeNoCompatiblePolicy], Vector[String]) = {
    val out = Vector.newBuilder[String]
    var runtime: Option[String] = None
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
    (runtime, selectionpolicy, nocompatiblepolicy, out.result())
  }

  private def _parse_execute(
    args: Vector[String],
    runtimeversion: Option[String],
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
    TextusCommand.Execute(mode, artifact, rest1.tail, runtimeversion, selectionpolicy, nocompatiblepolicy, effectivepassthrough)
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
      |  textus launcher version
      |  textus server  <artifact>[:<version>] [options...]
      |  textus client  <artifact>[:<version>] [args...]
      |  textus command <artifact>[:<version>] <operation> [params...]
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
      |
      |Artifact:
      |  textus-blog          auto-detect CAR/SAR from repositories
      |  textus-blog.car      force CAR
      |  textus-blog.sar      force SAR
      |  textus-blog:0.1.0    select artifact version
      |  --car textus-blog    force CAR
      |  --sar my-app         force SAR
      |
      |Compatibility:
      |  artifact@version remains accepted as a legacy spelling.
      |
      |Runtime:
      |  --runtime <version> overrides .textus/version and ~/.textus/version.
      |  Without --runtime, component runtime.cncf requirements use current-compatible selection by default.
      |  --runtime-selection=current-compatible|tested-latest|latest-compatible|newest-compatible selects the compatible runtime preference.
      |  --runtime-no-compatible=error|newest controls the fallback when no compatible runtime exists.
      |""".stripMargin
}

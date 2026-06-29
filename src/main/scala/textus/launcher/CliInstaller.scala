package textus.launcher

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

/*
 * @since   Jun. 29, 2026
 * @version Jun. 29, 2026
 * @author  ASAMI, Tomoharu
 */
object CliInstaller {
  def installTextus(
    paths: LauncherPaths,
    command: TextusCommand.InstallCli,
    config: LauncherConfig
  ): Path = {
    val bindir = command.binDir.map(p => paths.cwd.resolve(p).normalize).getOrElse(paths.home.resolve("bin"))
    val target = bindir.resolve(command.name)
    val configtarget = target.resolveSibling(s"${target.getFileName}.config.yaml")
    if (Files.exists(target) && !command.overwrite)
      throw TextusException(s"CLI command already exists: ${target}; use --overwrite")
    Files.createDirectories(bindir)
    Files.writeString(configtarget, _textus_config(config), StandardCharsets.UTF_8)
    Files.writeString(target, textusScript(configtarget, command), StandardCharsets.UTF_8)
    target.toFile.setExecutable(true, false)
    target
  }

  def textusScript(
    configpath: Path,
    command: TextusCommand.InstallCli
  ): String = {
    val fileparams = _file_param_aliases(command.fileParams)
    val fileparamcases = _file_param_cases(fileparams)
    val runtimeversion = command.runtimeVersion.getOrElse("")
    val runtimedevdir = command.runtimeDevDir.getOrElse("")
    s"""#!/usr/bin/env bash
       |set -euo pipefail
       |
       |operation_prefix='${_shell(command.operationPrefix)}'
       |launcher_config='${_shell(configpath.toAbsolutePath.normalize.toString)}'
       |artifact='${_shell(command.artifact.display)}'
       |runtime_version='${_shell(runtimeversion)}'
       |runtime_dev_dir='${_shell(runtimedevdir)}'
       |
       |usage() {
       |  cat <<'EOF'
       |Usage:
       |  ${command.name} <operation> [args...]
       |
       |Examples:
       |  ${command.name} validate-presentation --presentationDsl xxx.yaml
       |
       |This command delegates to:
       |  textus --config ${configpath.toAbsolutePath.normalize} ${_render_runtime_usage(command)} ${command.artifact.display} command ${command.operationPrefix}.<operation>
       |EOF
       |}
       |
       |is_file_param() {
       |  case "$$1" in
       |${fileparamcases}
       |      return 0
       |      ;;
       |    *)
       |      return 1
       |      ;;
       |  esac
       |}
       |
       |if [[ $$# -lt 1 ]]; then
       |  usage >&2
       |  exit 2
       |fi
       |
       |case "$$1" in
       |  -h|--help|help)
       |    usage
       |    exit 0
       |    ;;
       |esac
       |
       |operation="$$1"
       |shift
       |selector="$${operation_prefix}.$${operation}"
       |declare -a textus_args=()
       |if [[ -n "$$runtime_version" ]]; then
       |  textus_args+=("--runtime" "$$runtime_version")
       |fi
       |if [[ -n "$$runtime_dev_dir" ]]; then
       |  textus_args+=("--runtime-dev-dir" "$$runtime_dev_dir")
       |fi
       |
       |declare -a command_args=()
       |
       |while [[ $$# -gt 0 ]]; do
       |  case "$$1" in
       |    --*=*)
       |      option="$${1%%=*}"
       |      name="$${option#--}"
       |      value="$${1#*=}"
       |      shift
       |      if is_file_param "$$name" && [[ -f "$$value" ]]; then
       |        value="$$(<"$$value")"
       |      fi
       |      command_args+=("$$option" "$$value")
       |      ;;
       |    --*)
       |      option="$$1"
       |      name="$${option#--}"
       |      shift
       |      if is_file_param "$$name" && [[ $$# -gt 0 ]]; then
       |        value="$$1"
       |        shift
       |        if [[ -f "$$value" ]]; then
       |          value="$$(<"$$value")"
       |        fi
       |        command_args+=("$$option" "$$value")
       |      else
       |        command_args+=("$$option")
       |      fi
       |      ;;
       |    *)
       |      command_args+=("$$1")
       |      shift
       |      ;;
       |  esac
       |done
       |
       |exec textus --config "$$launcher_config" "$${textus_args[@]}" "$$artifact" command "$$selector" "$${command_args[@]}"
       |""".stripMargin
  }

  private def _textus_config(
    config: LauncherConfig
  ): String = {
    val cars = config.carRepositories.distinct.map(x => s"    - ${x}").mkString("\n")
    val sars = config.sarRepositories.distinct.map(x => s"    - ${x}").mkString("\n")
    s"""repositories:
       |  car:
       |${cars}
       |  sar:
       |${sars}
       |""".stripMargin
  }

  private def _render_runtime_usage(command: TextusCommand.InstallCli): String = {
    val args = Vector.newBuilder[String]
    command.runtimeVersion.foreach(v => args += s"--runtime ${v}")
    command.runtimeDevDir.foreach(v => args += s"--runtime-dev-dir ${v}")
    val rendered = args.result().mkString(" ")
    if (rendered.isEmpty) "" else s"${rendered} "
  }

  private def _file_param_aliases(values: Vector[String]): Vector[String] =
    values.flatMap(v => Vector(v, _camel_to_kebab(v))).distinct

  private def _file_param_cases(values: Vector[String]): String =
    if (values.isEmpty) "    __textus_no_file_params__)"
    else s"    ${values.map(_shell_case).mkString("|")})"

  private def _camel_to_kebab(value: String): String =
    value.flatMap { c =>
      if (c.isUpper) "-" + c.toLower.toString else c.toString
    }

  private def _shell(value: String): String =
    value.replace("'", "'\"'\"'")

  private def _shell_case(value: String): String =
    value.replace("\\", "\\\\").replace(")", "\\)")
}

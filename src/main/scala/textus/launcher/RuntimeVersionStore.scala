package textus.launcher

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

/*
 * @since   May. 17, 2026
 * @version May. 17, 2026
 * @author  ASAMI, Tomoharu
 */
final class RuntimeVersionStore(paths: LauncherPaths) {
  def current(cliversion: Option[String], config: LauncherConfig): String =
    cliversion
      .orElse(_read(paths.projectVersion))
      .orElse(_read(paths.globalVersion))
      .orElse(config.runtimeVersion)
      .getOrElse(LauncherConfig.DEFAULT_RUNTIME_VERSION)

  def readProject: Option[String] =
    _read(paths.projectVersion)

  def readGlobal: Option[String] =
    _read(paths.globalVersion)

  def useGlobal(version: String): Unit =
    _write(paths.globalVersion, version)

  def useProject(version: String): Unit =
    _write(paths.projectVersion, version)

  private def _read(path: Path): Option[String] =
    if (Files.isRegularFile(path))
      Some(Files.readString(path, StandardCharsets.UTF_8).trim).filter(_.nonEmpty)
    else
      None

  private def _write(path: Path, value: String): Unit = {
    Files.createDirectories(path.getParent)
    Files.writeString(path, value.trim + "\n", StandardCharsets.UTF_8)
  }
}

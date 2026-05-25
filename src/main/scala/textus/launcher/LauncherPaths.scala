package textus.launcher

import java.nio.file.{Path, Paths}

/*
 * @since   May. 17, 2026
 * @version May. 25, 2026
 * @author  ASAMI, Tomoharu
 */
final case class LauncherPaths(
  home: Path = Paths.get(sys.props.getOrElse("user.home", ".")).toAbsolutePath.normalize,
  cwd: Path = Paths.get("").toAbsolutePath.normalize
) {
  val textusHome: Path = home.resolve(".textus")
  val cncfHome: Path = home.resolve(".cncf")
  val globalConfig: Path = textusHome.resolve("config.yaml")
  val projectConfig: Path = cwd.resolve(".textus").resolve("config.yaml")
  val globalVersion: Path = textusHome.resolve("version")
  val projectVersion: Path = cwd.resolve(".textus").resolve("version")
  val runtimeRoot: Path = cncfHome.resolve("runtimes")
  val runtimeCatalog: Path = cncfHome.resolve("catalog").resolve("textus").resolve("runtime-catalog.yaml")
  val coursierCache: Path = cncfHome.resolve("cache").resolve("coursier")
  val localRepository: Path = cncfHome.resolve("local")
  val localCarRepository: Path = localRepository.resolve("repository").resolve("car")
  val localSarRepository: Path = localRepository.resolve("repository").resolve("sar")
  val cacheRepository: Path = cncfHome.resolve("cache")
  val cacheCarRepository: Path = cacheRepository.resolve("car")
  val cacheSarRepository: Path = cacheRepository.resolve("sar")
}

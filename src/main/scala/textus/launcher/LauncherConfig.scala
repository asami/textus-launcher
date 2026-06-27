package textus.launcher

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import org.goldenport.launcher.{LauncherConfigLoader => CoreLauncherConfigLoader, LauncherConfigParser => CoreLauncherConfigParser, LauncherCoreException, LauncherPaths => CoreLauncherPaths, LauncherProductSpec}

/*
 * @since   May. 17, 2026
 * @version Jun. 27, 2026
 * @author  ASAMI, Tomoharu
 */
final case class LauncherConfig(
  runtimeVersion: Option[String] = None,
  runtimeDevDir: Option[String] = None,
  developmentRuntimeDevDir: Option[String] = None,
  runtimeCatalogUrl: Option[String] = None,
  runtimeSelectionPolicy: Option[RuntimeSelectionPolicy] = None,
  runtimeNoCompatiblePolicy: Option[RuntimeNoCompatiblePolicy] = None,
  carRepositories: Vector[String] = Vector.empty,
  sarRepositories: Vector[String] = Vector.empty,
  mavenRepositories: Vector[String] = Vector.empty,
  coursierRepositories: Vector[String] = Vector.empty
) {
  def mergeHigher(higher: LauncherConfig): LauncherConfig =
    LauncherConfig(
      runtimeVersion = higher.runtimeVersion.orElse(runtimeVersion),
      runtimeDevDir = higher.runtimeDevDir.orElse(runtimeDevDir),
      developmentRuntimeDevDir = higher.developmentRuntimeDevDir.orElse(developmentRuntimeDevDir),
      runtimeCatalogUrl = higher.runtimeCatalogUrl.orElse(runtimeCatalogUrl),
      runtimeSelectionPolicy = higher.runtimeSelectionPolicy.orElse(runtimeSelectionPolicy),
      runtimeNoCompatiblePolicy = higher.runtimeNoCompatiblePolicy.orElse(runtimeNoCompatiblePolicy),
      carRepositories = _merge_list(carRepositories, higher.carRepositories),
      sarRepositories = _merge_list(sarRepositories, higher.sarRepositories),
      mavenRepositories = _merge_list(mavenRepositories, higher.mavenRepositories),
      coursierRepositories = _merge_list(coursierRepositories, higher.coursierRepositories)
    )

  def withDevelopmentEnabled: LauncherConfig =
    copy(runtimeDevDir = runtimeDevDir.orElse(developmentRuntimeDevDir))

  def normalizedWithDefaults: LauncherConfig =
    normalizedWithDefaults(LauncherPaths())

  def normalizedWithDefaults(paths: LauncherPaths): LauncherConfig =
    copy(
      carRepositories = _append_defaults(carRepositories, LauncherConfig.localCarRepositories(paths) ++ LauncherConfig.cacheCarRepositories(paths) ++ LauncherConfig.DEFAULT_CAR_REPOSITORIES),
      sarRepositories = _append_defaults(sarRepositories, LauncherConfig.localSarRepositories(paths) ++ LauncherConfig.cacheSarRepositories(paths) ++ LauncherConfig.DEFAULT_SAR_REPOSITORIES),
      mavenRepositories = _append_defaults(mavenRepositories, LauncherConfig.DEFAULT_MAVEN_REPOSITORIES),
      runtimeSelectionPolicy = runtimeSelectionPolicy.orElse(Some(RuntimeSelectionPolicy.CurrentCompatible)),
      runtimeNoCompatiblePolicy = runtimeNoCompatiblePolicy.orElse(Some(RuntimeNoCompatiblePolicy.Error)),
      runtimeCatalogUrl = runtimeCatalogUrl.orElse(Some(LauncherConfig.DEFAULT_RUNTIME_CATALOG_URL))
    )

  def withCatalog(catalog: RuntimeCatalog): LauncherConfig =
    copy(
      carRepositories = _merge_catalog_list(carRepositories, catalog.carRepositories, LauncherConfig.DEFAULT_CAR_REPOSITORIES),
      sarRepositories = _merge_catalog_list(sarRepositories, catalog.sarRepositories, LauncherConfig.DEFAULT_SAR_REPOSITORIES),
      mavenRepositories = _merge_catalog_list(mavenRepositories, catalog.mavenRepositories, LauncherConfig.DEFAULT_MAVEN_REPOSITORIES),
      coursierRepositories = _merge_catalog_list(coursierRepositories, catalog.coursierRepositories, Vector.empty)
    )

  private def _merge_list(
    lower: Vector[String],
    higher: Vector[String]
  ): Vector[String] =
    (higher ++ lower).distinct

  private def _append_defaults(
    configured: Vector[String],
    defaults: Vector[String]
  ): Vector[String] =
    configured ++ defaults.filterNot(configured.contains)

  private def _merge_catalog_list(
    configured: Vector[String],
    catalog: Vector[String],
    defaults: Vector[String]
  ): Vector[String] = {
    val explicit = configured.filterNot(defaults.contains)
    (explicit ++ catalog ++ defaults).distinct
  }
}

object LauncherConfig {
  val DEFAULT_RUNTIME_VERSION = "recommended"
  val DEFAULT_RUNTIME_CATALOG_URL = "https://www.simplemodeling.org/repository/textus/runtime-catalog.yaml"
  val DEFAULT_CAR_REPOSITORIES = Vector("https://www.simplemodeling.org/repository/car")
  val DEFAULT_SAR_REPOSITORIES = Vector("https://www.simplemodeling.org/repository/sar")
  val DEFAULT_MAVEN_REPOSITORIES = Vector("https://www.simplemodeling.org/repository/maven")

  private val _product_spec = LauncherProductSpec.textusConfig

  def load(paths: LauncherPaths): LauncherConfig =
    load(paths, Vector.empty)

  def load(
    paths: LauncherPaths,
    configfiles: Vector[String]
  ): LauncherConfig =
    load(paths, configfiles, sys.env)

  def load(
    paths: LauncherPaths,
    configfiles: Vector[String],
    environment: Map[String, String]
  ): LauncherConfig = {
    val corepaths = CoreLauncherPaths(home = paths.home, cwd = paths.cwd)
    val explicit =
      try {
        CoreLauncherConfigLoader.load(corepaths, _product_spec, configfiles).foldLeft(LauncherConfig()) { (acc, source) =>
          acc.mergeHigher(fromParsed(source.values))
        }
      } catch {
        case e: LauncherCoreException => throw new TextusException(e.getMessage, e.code)
      }
    val development =
      if (_use_development(environment))
        explicit.withDevelopmentEnabled
      else
        explicit
    development.mergeHigher(fromEnvironment(environment)).normalizedWithDefaults(paths)
  }

  def loadFile(path: Path): LauncherConfig =
    if (Files.isRegularFile(path)) {
      val text = Files.readString(path, StandardCharsets.UTF_8)
      fromParsed(LauncherConfigParser.parse(path, text))
    } else {
      LauncherConfig()
    }

  def loadRequiredFile(path: Path): LauncherConfig =
    if (Files.isRegularFile(path))
      loadFile(path)
    else
      throw TextusException(s"launcher config file not found: ${path}")

  def fromParsed(values: Map[String, Vector[String]]): LauncherConfig = {
    def _first_(keys: String*): Option[String] =
      keys.toVector.flatMap(k => values.getOrElse(k, Vector.empty)).headOption.map(_.trim).filter(_.nonEmpty)
    def _all_(keys: String*): Vector[String] =
      keys.toVector.flatMap(k => values.getOrElse(k, Vector.empty)).map(_.trim).filter(_.nonEmpty).distinct

    LauncherConfig(
      runtimeVersion = _first_("runtime.version", "textus.runtime.version", "version"),
      runtimeDevDir = _first_("runtime.dev-dir", "runtime.dev_dir", "textus.runtime.dev-dir", "textus.runtime.dev_dir", "runtime.devDir", "runtime.dev.dir", "textus.runtime.devDir", "textus.runtime.dev.dir"),
      developmentRuntimeDevDir = _first_("development.runtime.dev-dir", "development.runtime.dev_dir", "development.runtime.devDir", "development.runtime.dev.dir", "textus.development.runtime.dev-dir", "textus.development.runtime.dev_dir", "textus.development.runtime.devDir", "textus.development.runtime.dev.dir"),
      runtimeCatalogUrl = _first_("runtime.catalog.url", "textus.runtime.catalog.url", "catalog.url"),
      runtimeSelectionPolicy = _first_("runtime.cncf.selection-policy", "runtime.cncf.selection_policy", "runtime.cncf.selectionPolicy", "textus.runtime.cncf.selection-policy", "textus.runtime.cncf.selection_policy", "textus.runtime.cncf.selectionPolicy").
        map(RuntimeSelectionPolicy.parse),
      runtimeNoCompatiblePolicy = _first_("runtime.cncf.no-compatible-policy", "runtime.cncf.no_compatible_policy", "runtime.cncf.noCompatiblePolicy", "textus.runtime.cncf.no-compatible-policy", "textus.runtime.cncf.no_compatible_policy", "textus.runtime.cncf.noCompatiblePolicy").
        map(RuntimeNoCompatiblePolicy.parse),
      carRepositories = _all_("repositories.car", "componentRepositories.car", "textus.repository.car", "textus.component.repository.car"),
      sarRepositories = _all_("repositories.sar", "componentRepositories.sar", "textus.repository.sar", "textus.subsystem.repository.sar"),
      mavenRepositories = _all_("repositories.maven", "textus.repository.maven"),
      coursierRepositories = _all_("repositories.coursier", "textus.repository.coursier")
    )
  }

  def fromEnvironment(environment: Map[String, String]): LauncherConfig =
    LauncherConfig(
      runtimeDevDir = environment.get("TEXTUS_RUNTIME_DEV_DIR").orElse(environment.get("CNCF_RUNTIME_DEV_DIR"))
    )

  private def _use_development(environment: Map[String, String]): Boolean =
    environment.get("TEXTUS_USE_DEVELOPMENT")
      .orElse(environment.get("CNCF_USE_DEVELOPMENT"))
      .exists(value => Set("true", "yes", "on", "1").contains(value.trim.toLowerCase))

  def localCarRepositories(paths: LauncherPaths): Vector[String] =
    Vector(paths.localCarRepository.toString)

  def localSarRepositories(paths: LauncherPaths): Vector[String] =
    Vector(paths.localSarRepository.toString)

  def cacheCarRepositories(paths: LauncherPaths): Vector[String] =
    Vector(paths.cacheCarRepository.toString)

  def cacheSarRepositories(paths: LauncherPaths): Vector[String] =
    Vector(paths.cacheSarRepository.toString)

  def render(config: LauncherConfig): String = {
    val c =
      if (_has_local_repository(config))
        config
      else
        config.normalizedWithDefaults
    val runtime = c.runtimeVersion.getOrElse("(not configured)")
    val catalog = c.runtimeCatalogUrl.getOrElse("(not configured)")
    val selection = c.runtimeSelectionPolicy.map(RuntimeSelectionPolicy.render).getOrElse("current-compatible")
    val nocompatible = c.runtimeNoCompatiblePolicy.map(RuntimeNoCompatiblePolicy.render).getOrElse("error")
    val cars = c.carRepositories.mkString(", ")
    val sars = c.sarRepositories.mkString(", ")
    val mavens = c.mavenRepositories.mkString(", ")
    val coursiers = c.coursierRepositories.mkString(", ")
    val localrepository =
      c.carRepositories.find(_.contains("/.cncf/local/repository/car")).
        map(_.stripSuffix("/repository/car")).
        getOrElse("~/.cncf/local")
    val cacherepository =
      c.carRepositories.find(_.contains("/.cncf/cache/car")).
        map(_.stripSuffix("/car")).
        getOrElse("~/.cncf/cache")
    s"""runtime.version: $runtime
       |runtime.catalog.url: $catalog
       |runtime.cncf.selectionPolicy: $selection
       |runtime.cncf.noCompatiblePolicy: $nocompatible
       |local.repository: $localrepository
       |cache.repository: $cacherepository
       |local.repository.note: ~/.cncf/local is developer local publish state; ~/.cncf/cache is runtime-managed remote artifact cache
       |repositories.car: $cars
       |repositories.sar: $sars
       |repositories.maven: $mavens
       |repositories.coursier: $coursiers""".stripMargin
  }

  private def _has_local_repository(config: LauncherConfig): Boolean =
    (config.carRepositories ++ config.sarRepositories).exists(_.contains("/.cncf/local/repository/"))
}

object LauncherConfigParser {
  def parse(
    path: Path,
    text: String
  ): Map[String, Vector[String]] =
    CoreLauncherConfigParser.parse(path, text)
}

object SimpleYaml {
  def parse(text: String): Map[String, Vector[String]] =
    LauncherConfigParser.parse(Path.of("config.yaml"), text)
}

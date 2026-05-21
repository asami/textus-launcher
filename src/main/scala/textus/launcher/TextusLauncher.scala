package textus.launcher

import java.nio.file.Files

/*
 * @since   May. 17, 2026
 * @version May. 22, 2026
 * @author  ASAMI, Tomoharu
 */
final class TextusLauncher(
  paths: LauncherPaths = LauncherPaths(),
  runtimeresolver: CncfRuntimeResolver = CoursierCncfRuntimeResolver(),
  cncfinvoker: CncfInvoker = CncfInvoker()
) {
  def run(args: Vector[String]): Int = {
    val config = LauncherConfig.load(paths)
    val command = TextusCommandParser.parse(args)
    command match {
      case TextusCommand.Version =>
        println(s"${LauncherBuildInfo.name} ${LauncherBuildInfo.version}")
        0
      case TextusCommand.Help =>
        println(TextusCommandParser.helpText)
        0
      case runtime: TextusCommand.Runtime =>
        _run_runtime(runtime, config)
      case execute: TextusCommand.Execute =>
        _run_execute(execute, config)
    }
  }

  private def _run_runtime(
    command: TextusCommand.Runtime,
    config: LauncherConfig
  ): Int = {
    val store = RuntimeVersionStore(paths)
    val catalogstore = RuntimeCatalogStore(paths)
    command match {
      case TextusCommand.Runtime.Current =>
        println(runtimeresolver.resolveVersion(store.current(None, config), config, paths))
        0
      case TextusCommand.Runtime.LocalList =>
        val installed =
          if (Files.isDirectory(paths.runtimeRoot)) {
            val stream = Files.list(paths.runtimeRoot)
            try {
              import scala.jdk.CollectionConverters.*
              stream.iterator().asScala.filter(Files.isDirectory(_)).map(_.getFileName.toString).toVector.sorted
            } finally {
              stream.close()
            }
          } else {
            Vector.empty
          }
        installed.foreach(println)
        0
      case TextusCommand.Runtime.RemoteList =>
        val catalog = catalogstore.loadOrRefresh(config)
          .getOrElse(throw TextusException("failed to load Textus runtime catalog"))
        println(catalog.renderRemoteList)
        0
      case TextusCommand.Runtime.Refresh =>
        catalogstore.refresh(config)
        println(s"refreshed Textus runtime catalog: ${paths.runtimeCatalog}")
        0
      case TextusCommand.Runtime.CatalogShow =>
        val catalog = catalogstore.loadOrRefresh(config)
          .getOrElse(throw TextusException("failed to load Textus runtime catalog"))
        println(catalog.render)
        0
      case TextusCommand.Runtime.Channels =>
        val catalog = catalogstore.loadOrRefresh(config)
          .getOrElse(throw TextusException("failed to load Textus runtime catalog"))
        println(catalog.renderChannels)
        0
      case TextusCommand.Runtime.Install(version) =>
        val concreteversion = runtimeresolver.resolveVersion(version, config, paths)
        runtimeresolver.resolve(concreteversion, config, paths)
        println(s"installed CNCF runtime $concreteversion")
        0
      case TextusCommand.Runtime.Use(version, target) =>
        val concreteversion = runtimeresolver.resolveVersion(version, config, paths)
        val resolvedtarget = _resolve_runtime_use_target(target)
        resolvedtarget match {
          case TextusCommand.RuntimeUseTarget.Global => store.useGlobal(version)
          case TextusCommand.RuntimeUseTarget.Project => store.useProject(version)
          case TextusCommand.RuntimeUseTarget.Auto => throw TextusException("unresolved runtime use target")
        }
        println(s"using CNCF runtime $version -> $concreteversion (${resolvedtarget.toString.toLowerCase})")
        0
      case TextusCommand.Runtime.CacheStatus() =>
        println(s"textus home: ${paths.textusHome}")
        println(s"cncf home: ${paths.cncfHome}")
        println(s"local repository: ${paths.localRepository}")
        println(s"runtime cache: ${paths.runtimeRoot}")
        println(s"coursier cache: ${paths.coursierCache}")
        0
      case TextusCommand.Runtime.ConfigShow() =>
        println(LauncherConfig.render(config))
        0
    }
  }

  private def _resolve_runtime_use_target(
    target: TextusCommand.RuntimeUseTarget
  ): TextusCommand.RuntimeUseTarget =
    target match {
      case TextusCommand.RuntimeUseTarget.Auto =>
        if (Files.exists(paths.cwd.resolve(".textus")))
          TextusCommand.RuntimeUseTarget.Project
        else
          TextusCommand.RuntimeUseTarget.Global
      case x => x
    }

  private def _run_execute(
    command: TextusCommand.Execute,
    config: LauncherConfig
  ): Int = {
    val store = RuntimeVersionStore(paths)
    val catalog = RuntimeCatalogStore(paths).loadOrRefresh(config)
    val effectiveconfig = catalog.map(config.withCatalog).getOrElse(config)
    val resolved = ArtifactResolver().resolve(command.artifact, effectiveconfig)
    val selectionpolicy = command.runtimeSelectionPolicy.
      orElse(effectiveconfig.runtimeSelectionPolicy).
      getOrElse(RuntimeSelectionPolicy.CurrentCompatible)
    val policy = command.runtimeNoCompatiblePolicy.orElse(effectiveconfig.runtimeNoCompatiblePolicy).getOrElse(RuntimeNoCompatiblePolicy.Error)
    val runtimeversion = RuntimeVersionSelection.select(
      requested = command.runtimeVersion,
      stored = store.current(None, config),
      requirements = resolved.runtimeRequirements,
      catalog = catalog,
      selectionPolicy = selectionpolicy,
      policy = policy
    )
    val cncfargs = _cncf_args(command, resolved, effectiveconfig)
    val classpath = runtimeresolver.resolve(runtimeversion, effectiveconfig, paths)
    cncfinvoker.invoke(classpath, cncfargs)
  }

  private def _cncf_args(
    command: TextusCommand.Execute,
    artifact: ResolvedArtifact,
    config: LauncherConfig
  ): Vector[String] = {
    val repositoryargs =
      config.carRepositories.map(r => s"--repository-dir=$r") ++
        config.sarRepositories.map(r => s"--repository-dir=$r")
    val artifactargs =
      artifact.kind match {
        case ArtifactKind.Car =>
          Vector(s"--textus.component=${artifact.selector.name}") ++
            artifact.selector.version.map(v => s"--textus.component.version=$v").toVector
        case ArtifactKind.Sar =>
          val name = artifact.selector.version.map(v => s"${artifact.selector.name}-$v").getOrElse(artifact.selector.name)
          Vector(s"--textus.subsystem=$name")
        case ArtifactKind.Auto =>
          throw TextusException("unresolved artifact kind")
      }
    repositoryargs ++ artifactargs ++ Vector(command.mode) ++ command.args ++ command.passthrough
  }
}

object TextusLauncher {
  def apply(): TextusLauncher =
    new TextusLauncher()
}

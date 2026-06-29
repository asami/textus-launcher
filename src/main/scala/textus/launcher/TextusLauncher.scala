package textus.launcher

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.sys.process.*

/*
 * @since   May. 17, 2026
 *  version May. 27, 2026
 * @version Jun. 29, 2026
 * @author  ASAMI, Tomoharu
 */
final class TextusLauncher(
  paths: LauncherPaths = LauncherPaths(),
  runtimeresolver: CncfRuntimeResolver = CoursierCncfRuntimeResolver(),
  cncfinvoker: CncfInvoker = CncfInvoker(),
  classpathexporter: RuntimeClasspathExporter = SbtRuntimeClasspathExporter
) {
  def run(args: Vector[String]): Int = {
    val (configfiles, commandargs) = _take_config_options(args)
    val config = LauncherConfig.load(paths, configfiles)
    val command = TextusCommandParser.parse(commandargs)
    command match {
      case TextusCommand.LauncherVersion =>
        println(s"${LauncherBuildInfo.name} ${LauncherBuildInfo.version}")
        0
      case TextusCommand.RuntimeHelp =>
        val code = _run_runtime_help(config)
        println()
        println("Launcher help:")
        println(TextusCommandParser.helpText)
        code
      case TextusCommand.LauncherHelp =>
        println(TextusCommandParser.helpText)
        0
      case runtime: TextusCommand.Runtime =>
        _run_runtime(runtime, config)
      case install: TextusCommand.InstallCli =>
        _run_install_cli(install, config)
      case execute: TextusCommand.Execute =>
        _run_execute(execute, config)
    }
  }

  private def _run_install_cli(
    command: TextusCommand.InstallCli,
    config: LauncherConfig
  ): Int = {
    val pinned = _pin_install_cli_runtime(command, config)
    val path = CliInstaller.installTextus(paths, pinned)
    println(s"installed CLI command ${pinned.name}: ${path}")
    0
  }

  private def _pin_install_cli_runtime(
    command: TextusCommand.InstallCli,
    config: LauncherConfig
  ): TextusCommand.InstallCli = {
    val store = RuntimeVersionStore(paths)
    val catalog = RuntimeCatalogStore(paths).loadOrRefresh(config)
    val effectiveconfig = catalog.map(config.withCatalog).getOrElse(config)
    val resolved = ArtifactResolver().resolve(command.artifact, effectiveconfig)
    val selectionpolicy = command.runtimeSelectionPolicy.
      orElse(effectiveconfig.runtimeSelectionPolicy).
      getOrElse(RuntimeSelectionPolicy.CurrentCompatible)
    val policy = command.runtimeNoCompatiblePolicy.orElse(effectiveconfig.runtimeNoCompatiblePolicy).getOrElse(RuntimeNoCompatiblePolicy.Error)
    val runtimedevdir = command.runtimeDevDir.orElse(config.runtimeDevDir)
    val runtimeversion = RuntimeVersionSelection.select(
      requested = command.runtimeVersion,
      stored = store.current(None, config),
      requirements = resolved.runtimeRequirements,
      catalog = catalog,
      selectionPolicy = selectionpolicy,
      policy = policy
    )
    command.copy(
      artifact = resolved.selector,
      runtimeVersion = if (runtimedevdir.isDefined) None else Some(runtimeversion),
      runtimeDevDir = runtimedevdir,
      runtimeSelectionPolicy = None,
      runtimeNoCompatiblePolicy = None
    )
  }

  private def _run_runtime_help(config: LauncherConfig): Int = {
    val store = RuntimeVersionStore(paths)
    val runtimeversion = store.current(None, config)
    val classpath = runtimeresolver.resolve(runtimeversion, config, paths)
    cncfinvoker.invoke(classpath, Vector("--help"))
  }

  private def _run_runtime(
    command: TextusCommand.Runtime,
    config: LauncherConfig
  ): Int = {
    val store = RuntimeVersionStore(paths)
    val catalogstore = RuntimeCatalogStore(paths)
    command match {
      case TextusCommand.Runtime.Version(runtimeversion, runtimedevdir) =>
        _run_runtime_version(runtimeversion, runtimedevdir, store, config)
      case TextusCommand.Runtime.Current =>
        _run_runtime_current(store, catalogstore, config)
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
        println(s"artifact cache: ${paths.cacheRepository}")
        println(s"runtime cache: ${paths.runtimeRoot}")
        println(s"coursier cache: ${paths.coursierCache}")
        0
      case TextusCommand.Runtime.ConfigShow() =>
        println(LauncherConfig.render(config))
        0
    }
  }

  private def _run_runtime_version(
    runtimeversion: Option[String],
    runtimedevdir: Option[String],
    store: RuntimeVersionStore,
    config: LauncherConfig
  ): Int = {
    val classpath = _runtime_classpath(runtimeversion, runtimedevdir, store, config)
    cncfinvoker.invoke(classpath, Vector("version"))
  }

  private def _runtime_classpath(
    runtimeversion: Option[String],
    runtimedevdir: Option[String],
    store: RuntimeVersionStore,
    config: LauncherConfig
  ): Vector[Path] =
    runtimedevdir.orElse(config.runtimeDevDir) match {
      case Some(dir) =>
        _development_runtime_classpath(paths.cwd.resolve(dir).normalize.toAbsolutePath.normalize)
      case None =>
        val selector = store.current(runtimeversion, config)
        runtimeresolver.resolve(selector, config, paths)
    }

  private def _development_runtime_classpath(project: Path): Vector[Path] = {
    val file = project.resolve("target").resolve("cncf.d").resolve("runtime-classpath.txt")
    val classpath =
      if (Files.isRegularFile(file) && Files.size(file) > 0L) {
        Files.readString(file, StandardCharsets.UTF_8).trim
      } else {
        val exported = classpathexporter.exportRuntimeClasspath(project)
        Files.createDirectories(file.getParent)
        Files.writeString(file, exported + "\n", StandardCharsets.UTF_8)
        exported
      }
    val entries = classpath.split(File.pathSeparator).toVector.map(_.trim).filter(_.nonEmpty).map(Path.of(_))
    if (entries.isEmpty)
      throw TextusException(s"CNCF Runtime / fullClasspath was empty for ${project}")
    entries
  }

  private def _run_runtime_current(
    store: RuntimeVersionStore,
    catalogstore: RuntimeCatalogStore,
    config: LauncherConfig
  ): Int = {
    val selector = store.current(None, config)
    val current = runtimeresolver.resolveVersion(selector, config, paths)
    println(current)
    _warn_if_runtime_catalog_is_stale(selector, current, catalogstore, config)
    0
  }

  private def _warn_if_runtime_catalog_is_stale(
    selector: String,
    current: String,
    catalogstore: RuntimeCatalogStore,
    config: LauncherConfig
  ): Unit =
    if (_is_dynamic_runtime_selector(selector) && !_is_dynamic_runtime_selector(current)) {
      val remoteversion =
        try Some(catalogstore.fetch(config).resolve(selector).version)
        catch {
          case _: Throwable => None
        }
      remoteversion.filter(_ != current).foreach { version =>
        Console.err.println(
          s"warning: cached Textus runtime catalog resolves $selector to $current, but remote catalog resolves it to $version."
        )
        Console.err.println("Run 'textus runtime refresh' to update the local runtime catalog cache.")
      }
    }

  private def _is_dynamic_runtime_selector(selector: String): Boolean =
    selector match {
      case "recommended" | "latest" | "latest-stable" | "latest.release" | "latest-snapshot" | "newest" => true
      case _ => false
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

  private def _take_config_options(args: Vector[String]): (Vector[String], Vector[String]) = {
    val configfiles = Vector.newBuilder[String]
    val commandargs = Vector.newBuilder[String]
    var i = 0
    var passthrough = false
    while (i < args.length) {
      if (passthrough) {
        commandargs += args(i)
        i += 1
      } else {
        args(i) match {
          case "--" =>
            passthrough = true
            commandargs += args(i)
            i += 1
          case "--config" | "--launcher-config" =>
            if (i + 1 >= args.length)
              throw TextusException(s"${args(i)} requires a file")
            configfiles += args(i + 1)
            i += 2
          case x if x.startsWith("--config=") =>
            configfiles += x.stripPrefix("--config=")
            i += 1
          case x if x.startsWith("--launcher-config=") =>
            configfiles += x.stripPrefix("--launcher-config=")
            i += 1
          case x =>
            commandargs += x
            i += 1
        }
      }
    }
    (configfiles.result(), commandargs.result())
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
    val classpath = command.runtimeDevDir.orElse(config.runtimeDevDir) match {
      case Some(dir) => _development_runtime_classpath(paths.cwd.resolve(dir).normalize.toAbsolutePath.normalize)
      case None => runtimeresolver.resolve(runtimeversion, effectiveconfig, paths)
    }
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

package textus.launcher

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.regex.Pattern
import scala.sys.process.*

/*
 * @since   May. 17, 2026
 *  version May. 27, 2026
 *  version Jun. 29, 2026
 *  version Jul. 24, 2026
 * @version Aug. 11, 2026
 * @author  ASAMI, Tomoharu
 */
final class TextusLauncher(
  paths: LauncherPaths = LauncherPaths(),
  runtimeresolver: CncfRuntimeResolver = CoursierCncfRuntimeResolver(),
  cncfinvoker: CncfInvoker = CncfInvoker(),
  launcherdevinvoker: TextusLauncherDevInvoker = TextusLauncherDevInvoker.System,
  environment: Map[String, String] = sys.env,
  registrationreporter: TextusControlCenterRegistrationReporter = TextusControlCenterRegistrationReporter.System
) {
  def run(args: Vector[String]): Int =
    _launcher_home(args).fold(_run(args)) { case (home, commandargs) =>
      new TextusLauncher(paths.copy(home = home), runtimeresolver, cncfinvoker, launcherdevinvoker, environment, registrationreporter).run(commandargs)
    }

  private def _run(args: Vector[String]): Int = {
    val (configfiles, commandargs) = _take_config_options(args)
    val config = LauncherConfig.load(paths, configfiles, environment)
    _delegate_launcher_dev_dir(config, args) match {
      case Some(code) => return code
      case None => ()
    }
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
      case repository: TextusCommand.Repository =>
        _run_repository(repository, config)
      case install: TextusCommand.InstallCli =>
        _run_install_cli(install, config)
      case execute: TextusCommand.Execute =>
        _run_execute(execute, config)
    }
  }

  private def _launcher_home(args: Vector[String]): Option[(Path, Vector[String])] = {
    val launcherscope = args.take(args.indexOf("--") match {
      case -1 => args.length
      case index => index
    })
    val index = launcherscope.indexOf("--launcher-home")
    if (index < 0) None
    else if (index + 1 >= args.length) throw TextusException("--launcher-home requires a directory")
    else if (launcherscope.indexOf("--launcher-home", index + 1) >= 0) throw TextusException("--launcher-home may be specified only once")
    else Some(paths.cwd.resolve(args(index + 1)).normalize.toAbsolutePath.normalize -> (args.take(index) ++ args.drop(index + 2)))
  }

  private def _run_repository(command: TextusCommand.Repository, config: LauncherConfig): Int = {
    val discovery = ComponentRepositoryDiscovery(paths)
    command match {
      case TextusCommand.Repository.ListArtifacts(kind, source) =>
        val result = discovery.list(config, kind, source)
        result.diagnostics.foreach(message => Console.err.println(s"warning: $message"))
        result.artifacts.foreach(artifact => println(artifact.render))
        0
      case TextusCommand.Repository.Show(artifactid, kind, source) =>
        val result = discovery.show(config, artifactid, kind, source)
        result.diagnostics.foreach(message => Console.err.println(s"warning: $message"))
        println(result.artifact.renderDetailed)
        0
      case TextusCommand.Repository.Refresh(source) =>
        val result = discovery.refresh(config, source)
        result.diagnostics.foreach(message => Console.err.println(s"warning: $message"))
        result.refreshed.foreach(value => println(s"refreshed component repository index: $value"))
        if (result.failures.nonEmpty) {
          result.failures.foreach(message => Console.err.println(s"error: $message"))
          1
        } else 0
    }
  }

  private def _delegate_launcher_dev_dir(
    config: LauncherConfig,
    args: Vector[String]
  ): Option[Int] =
    if (environment.get("TEXTUS_LAUNCHER_DEV_DELEGATED").contains("1"))
      None
    else
      config.launcherDevDir.map { dir =>
        val path = paths.cwd.resolve(dir).normalize.toAbsolutePath.normalize
        launcherdevinvoker.invoke(path, args, paths.cwd.toAbsolutePath.normalize)
      }

  private def _run_install_cli(
    command: TextusCommand.InstallCli,
    config: LauncherConfig
  ): Int = {
    val (pinned, effectiveconfig) = _pin_install_cli_runtime(command, config)
    val path = CliInstaller.installTextus(paths, pinned, effectiveconfig)
    println(s"installed CLI command ${pinned.name}: ${path}")
    0
  }

  private def _pin_install_cli_runtime(
    command: TextusCommand.InstallCli,
    config: LauncherConfig
  ): (TextusCommand.InstallCli, LauncherConfig) = {
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
    val pinned = command.copy(
        artifact = resolved.selector,
        runtimeVersion = if (runtimedevdir.isDefined) None else Some(runtimeversion),
        runtimeDevDir = runtimedevdir.map(p => paths.cwd.resolve(p).normalize.toAbsolutePath.normalize.toString),
        runtimeSelectionPolicy = None,
        runtimeNoCompatiblePolicy = None
      )
    (pinned, effectiveconfig)
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
        throw TextusException(s"development runtime classpath not found: ${file}; prepare it before invoking textus")
      }
    val entries = classpath
      .linesIterator
      .flatMap(_.split(Pattern.quote(File.pathSeparator)))
      .toVector
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(Path.of(_))
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
    val report = _server_report(command, resolved, runtimeversion)
    val evidencesession = report.map(_local_evidence_session).getOrElse(LocalServerEvidenceSession.noop)
    val registrationsession = report.map(_registration_session(command, effectiveconfig, _)).getOrElse(TextusControlCenterRegistrationSession.noop)
    val shutdownhook = new Thread(
      () => {
        registrationsession.close()
        evidencesession.close()
      },
      "textus-server-lifecycle-shutdown"
    )
    Runtime.getRuntime.addShutdownHook(shutdownhook)
    try {
      cncfinvoker.invoke(classpath, cncfargs)
    } finally {
      scala.util.Try(Runtime.getRuntime.removeShutdownHook(shutdownhook))
      registrationsession.close()
      evidencesession.close()
    }
  }

  private def _server_report(
    command: TextusCommand.Execute,
    artifact: ResolvedArtifact,
    runtimeversion: String
  ): Option[TextusControlCenterRegistrationReport] =
    if (command.mode != "server") {
      None
    } else {
      Some(TextusControlCenterRegistrationReport(
        instanceId = java.util.UUID.randomUUID().toString,
        target = artifact.selector.name,
        artifactId = Some(artifact.selector.name),
        executionMode = "artifact",
        developmentDirectory = None,
        subsystemName = Some(artifact.selector.name),
        subsystemVersion = artifact.selector.version,
        runtimeVersion = runtimeversion,
        startedAt = java.time.Instant.now()
      ))
    }

  private def _local_evidence_session(
    report: TextusControlCenterRegistrationReport
  ): LocalServerEvidenceSession =
    try {
      LocalServerEvidenceSession.start(paths, report, "textus")
    } catch {
      case _: Throwable =>
        Console.err.println("warning: local CNCF server evidence could not be recorded; continuing server startup.")
        LocalServerEvidenceSession.noop
    }

  private def _registration_session(
    command: TextusCommand.Execute,
    config: LauncherConfig,
    report: TextusControlCenterRegistrationReport
  ): TextusControlCenterRegistrationSession = {
      val registration = config.textusControlCenterRegistration.map(value => value -> environment.get(value.tokenEnv)).orElse {
        Option.when(!config.textusControlCenterRegistrationEnabled.contains(false))(
          TextusControlCenterStandaloneLocator.resolve(paths).map { value =>
            val configuration = _standalone_registration_base_url(command).fold(value.config)(baseurl => value.config.copy(baseUrl = baseurl))
            configuration -> Some(value.token)
          }
        ).flatten
      }
      registration match {
        case Some((configuration, token)) =>
          try {
            registrationreporter.start(
              configuration,
              report,
              token
            )
          } catch {
            case _: Throwable =>
              Console.err.println("warning: Textus Control Center registration setup failed; continuing server startup.")
              TextusControlCenterRegistrationSession.noop
          }
        case None => TextusControlCenterRegistrationSession.noop
      }
    }

  private def _standalone_registration_base_url(command: TextusCommand.Execute): Option[String] =
    (command.args ++ command.passthrough).collectFirst {
      case value if value.startsWith("--textus.server.port=") => value.stripPrefix("--textus.server.port=")
      case value if value.startsWith("--cncf.server.port=") => value.stripPrefix("--cncf.server.port=")
    }.flatMap { value =>
      scala.util.Try(value.toInt).toOption.filter(port => port >= 1 && port <= 65535).map(port => s"http://127.0.0.1:$port")
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

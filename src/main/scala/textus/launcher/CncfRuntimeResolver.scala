package textus.launcher

import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.sys.process.*
import scala.util.Using

/*
 * @since   May. 17, 2026
 * @version Jun. 27, 2026
 * @author  ASAMI, Tomoharu
 */
trait CncfRuntimeResolver {
  def resolveVersion(version: String, config: LauncherConfig, paths: LauncherPaths): String =
    version

  def resolve(version: String, config: LauncherConfig, paths: LauncherPaths): Vector[Path]
}


trait RuntimeClasspathExporter {
  def exportRuntimeClasspath(project: Path): String
}

object SbtRuntimeClasspathExporter extends RuntimeClasspathExporter {
  def exportRuntimeClasspath(project: Path): String = {
    val out = new StringBuilder
    val err = new StringBuilder
    val code = Process(Vector("sbt", "--batch", "export Runtime / fullClasspath"), project.toFile)
      .!(ProcessLogger(line => out.append(line).append("\n"), line => err.append(line).append("\n")))
    if (code != 0)
      throw TextusException(s"failed to resolve Runtime / fullClasspath for ${project}: ${err.toString.trim}", 2)
    out.toString.linesIterator
      .map(_.trim)
      .find(line => line.startsWith("/") && line.contains(File.pathSeparator))
      .orElse(out.toString.linesIterator.map(_.trim).find(_.startsWith("/")))
      .getOrElse(throw TextusException(s"failed to find classpath in sbt output for ${project}", 2))
  }
}

final class CoursierCncfRuntimeResolver(
  coursiercommand: String = sys.env.getOrElse("TEXTUS_COURSIER_COMMAND", "cs")
) extends CncfRuntimeResolver {
  override def resolveVersion(version: String, config: LauncherConfig, paths: LauncherPaths): String =
    _catalog_version(version, config, paths) match {
      case Some(v) =>
        v.warnIfDeprecated()
        v.version
      case None =>
        _fallback_version(version, config)
    }

  def resolve(version: String, config: LauncherConfig, paths: LauncherPaths): Vector[Path] = {
    val catalog = RuntimeCatalogStore(paths).loadOrRefresh(config)
    val catalogversion = _catalog_version(version, config, paths, catalog)
    val concreteversion =
      catalogversion match {
        case Some(v) =>
          v.warnIfDeprecated()
          v.version
        case None =>
          _fallback_version(version, config)
      }
    val metadata = paths.runtimeRoot.resolve(concreteversion).resolve("classpath.txt")
    if (Files.isRegularFile(metadata)) {
      _read_classpath(metadata)
    } else {
      Files.createDirectories(metadata.getParent)
      Files.createDirectories(paths.coursierCache)
      val module = catalogversion.map(_.moduleCoordinate).getOrElse(s"org.goldenport:goldenport-cncf_3:$concreteversion")
      val effectiveconfig = catalog.map(config.withCatalog).getOrElse(config)
      val repositories = (effectiveconfig.coursierRepositories ++ effectiveconfig.mavenRepositories).distinct.flatMap(r => Vector("-r", r))
      val command =
        Vector(coursiercommand, "fetch", "--classpath", "--cache", paths.coursierCache.toString) ++
          repositories ++
          Vector(module)
      val out = new StringBuilder
      val err = new StringBuilder
      val code = Process(command).!(ProcessLogger(out append _, err append _))
      if (code != 0) {
        throw TextusException(
          s"failed to resolve CNCF runtime $concreteversion with Coursier: ${err.toString.trim}",
          2
        )
      }
      val classpath = out.toString.trim
      if (classpath.isEmpty)
        throw TextusException(s"Coursier returned an empty classpath for CNCF runtime $concreteversion", 2)
      Files.writeString(metadata, classpath + "\n", StandardCharsets.UTF_8)
      _classpath_to_paths(classpath)
    }
  }

  private def _catalog_version(
    version: String,
    config: LauncherConfig,
    paths: LauncherPaths
  ): Option[RuntimeCatalogVersion] =
    _catalog_version(version, config, paths, RuntimeCatalogStore(paths).loadOrRefresh(config))

  private def _catalog_version(
    version: String,
    config: LauncherConfig,
    paths: LauncherPaths,
    catalog: Option[RuntimeCatalog]
  ): Option[RuntimeCatalogVersion] =
    catalog.map(_.resolve(version))

  private def _fallback_version(version: String, config: LauncherConfig): String =
    version match {
      case "latest" | "latest-stable" | "latest.release" =>
        _latest_release(config)
      case "latest-snapshot" =>
        _latest_snapshot(config)
      case "newest" =>
        _newest(config)
      case "recommended" =>
        throw TextusException("failed to resolve recommended CNCF runtime version from runtime catalog")
      case x =>
        x
    }

  private def _newest(config: LauncherConfig): String =
    config.mavenRepositories.iterator.flatMap(_metadata_versions).nextOption()
      .getOrElse(throw TextusException("failed to resolve newest CNCF runtime version from Maven repositories"))

  private def _latest_release(config: LauncherConfig): String =
    config.mavenRepositories.iterator.flatMap(_metadata_versions).find(!_.endsWith("-SNAPSHOT"))
      .getOrElse(throw TextusException("failed to resolve latest CNCF runtime version from Maven repositories"))

  private def _latest_snapshot(config: LauncherConfig): String =
    config.mavenRepositories.iterator.flatMap(_metadata_versions).find(_.endsWith("-SNAPSHOT"))
      .getOrElse(throw TextusException("failed to resolve latest snapshot CNCF runtime version from Maven repositories"))

  private def _metadata_versions(repository: String): Vector[String] = {
    val url = _join(repository, "org", "goldenport", "goldenport-cncf_3", "maven-metadata.xml")
    try {
      val connection = URI.create(url).toURL.openConnection()
      connection.setConnectTimeout(2000)
      connection.setReadTimeout(5000)
      val text = Using.resource(scala.io.Source.fromInputStream(connection.getInputStream, "UTF-8"))(_.mkString)
      val latest = _first_tag(text, "latest").orElse(_first_tag(text, "release")).toVector
      val versions = "<version>([^<]+)</version>".r.findAllMatchIn(text).map(_.group(1).trim).filter(_.nonEmpty).toVector.reverse
      (latest ++ versions).distinct
    } catch {
      case _: Throwable => Vector.empty
    }
  }

  private def _read_classpath(path: Path): Vector[Path] =
    _classpath_to_paths(Files.readString(path, StandardCharsets.UTF_8).trim)

  private def _classpath_to_paths(value: String): Vector[Path] =
    value.split(File.pathSeparator).toVector.map(_.trim).filter(_.nonEmpty).map(Path.of(_))

  private def _join(parts: String*): String =
    parts.toVector.zipWithIndex.map { case (p, idx) =>
      if (idx == 0) p.reverse.dropWhile(_ == '/').reverse
      else p.dropWhile(_ == '/').reverse.dropWhile(_ == '/').reverse
    }.mkString("/")

  private def _first_tag(text: String, tag: String): Option[String] =
    s"<$tag>([^<]+)</$tag>".r.findFirstMatchIn(text).map(_.group(1).trim).filter(_.nonEmpty)
}

class CncfInvoker {
  def invoke(classpath: Vector[Path], args: Vector[String]): Int = {
    val urls = classpath.map(_.toUri.toURL).toArray
    val parent = ClassLoader.getPlatformClassLoader
    val loader = new java.net.URLClassLoader(urls, parent)
    val old = Thread.currentThread().getContextClassLoader
    try {
      Thread.currentThread().setContextClassLoader(loader)
      val mainclass = Class.forName("org.goldenport.cncf.CncfMain", true, loader)
      val main = mainclass.getMethod("main", classOf[Array[String]])
      main.invoke(null, args.toArray)
      0
    } catch {
      case e: java.lang.reflect.InvocationTargetException =>
        val cause = Option(e.getCause).getOrElse(e)
        val clifailed =
          cause.getClass.getName == "org.goldenport.cncf.CncfMain$CliFailed"
        if (clifailed) {
          val method = cause.getClass.getMethod("code")
          method.invoke(cause).asInstanceOf[Int]
        } else {
          throw cause
        }
    } finally {
      Thread.currentThread().setContextClassLoader(old)
      loader.close()
    }
  }
}

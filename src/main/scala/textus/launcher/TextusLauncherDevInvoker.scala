package textus.launcher

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.zip.ZipFile
import scala.util.{Try, Using}
import org.goldenport.launcher.{LauncherDevInvoker => CoreLauncherDevInvoker}

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
trait TextusLauncherDevInvoker {
  def invoke(devdir: Path, args: Vector[String], cwd: Path): Int
}

object TextusLauncherDevInvoker {
  object System extends TextusLauncherDevInvoker {
    def invoke(devdir: Path, args: Vector[String], cwd: Path): Int = {
      if (!Files.isDirectory(devdir))
        throw TextusException(s"textus launcher development directory not found: ${devdir}")
      val classpath = _launcher_classpath(devdir)
      CoreLauncherDevInvoker.invokeJavaMain(
        productname = "textus",
        devdir = devdir,
        classpath = classpath,
        mainclass = "textus.launcher.TextusMain",
        args = args,
        cwd = cwd,
        environment = Map("TEXTUS_LAUNCHER_DEV_DELEGATED" -> "1"),
        argsFileEnvironmentKey = "TEXTUS_LAUNCHER_ARGS_FILE"
      )
    }

    private def _launcher_classpath(devdir: Path): String = {
      val file = devdir.resolve("target").resolve("textus.d").resolve("runtime-classpath.txt")
      if (!Files.isRegularFile(file))
        throw TextusException(s"textus launcher development classpath not found: ${file}; run sbt textusExportLauncherClasspath in ${devdir}")
      val value = Files.readString(file, StandardCharsets.UTF_8).trim
      if (value.isEmpty)
        throw TextusException(s"textus launcher development classpath is empty: ${file}")
      if (!_contains_launcher_main(value))
        throw TextusException(s"textus launcher development classpath does not contain textus.launcher.TextusMain: ${file}; run sbt textusExportLauncherClasspath in ${devdir}")
      value
    }

    private def _contains_launcher_main(classpath: String): Boolean =
      classpath.split(File.pathSeparator).toVector.map(_.trim).filter(_.nonEmpty).exists { entry =>
        val path = Path.of(entry)
        if (Files.isDirectory(path))
          Files.isRegularFile(path.resolve("textus").resolve("launcher").resolve("TextusMain.class"))
        else if (Files.isRegularFile(path) && entry.endsWith(".jar"))
          _jar_contains_launcher_main(path)
        else
          false
      }

    private def _jar_contains_launcher_main(path: Path): Boolean =
      Try {
        Using.resource(new ZipFile(path.toFile)) { jar =>
          jar.getEntry("textus/launcher/TextusMain.class") != null
        }
      }.getOrElse(false)
  }
}

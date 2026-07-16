package textus.launcher

import java.nio.charset.StandardCharsets
import java.nio.file.Files

/*
 * @since   May. 17, 2026
 *  version May. 17, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
object TextusMain {
  def main(args: Array[String]): Unit = {
    val effectiveargs = _args_from_file().getOrElse(args.toVector)
    val code =
      try TextusLauncher().run(effectiveargs)
      catch {
        case e: TextusException =>
          Console.err.println(e.getMessage)
          e.code
        case e: Throwable =>
          Console.err.println(e.getMessage)
          1
      }
    if (code != 0)
      sys.exit(code)
  }

  private def _args_from_file(): Option[Vector[String]] =
    sys.env.get("TEXTUS_LAUNCHER_ARGS_FILE").map { filename =>
      val bytes = Files.readAllBytes(java.nio.file.Paths.get(filename))
      if (bytes.isEmpty)
        Vector.empty
      else
        new String(bytes, StandardCharsets.UTF_8).split("\u0000", -1).toVector.dropRight(1)
    }
}

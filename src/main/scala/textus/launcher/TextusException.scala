package textus.launcher

/*
 * @since   May. 17, 2026
 * @version May. 17, 2026
 * @author  ASAMI, Tomoharu
 */
final class TextusException(
  message: String,
  val code: Int = 2
) extends RuntimeException(message)

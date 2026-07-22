package textus.launcher

import java.nio.charset.StandardCharsets
import java.nio.channels.FileChannel
import java.nio.file.{AtomicMoveNotSupportedException, Files, StandardCopyOption}
import java.nio.file.StandardOpenOption.{CREATE, WRITE}
import java.time.{Duration, Instant}
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{Executors, TimeUnit}

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.decode
import io.circe.syntax.*

/*
 * A launcher-owned, Control-Center-independent server lifecycle record.
 * Its JSON schema and ~/.cncf/launcher location are shared with cncf-launcher.
 */
final case class LocalServerEvidenceEntry(
  launcherKind: String,
  instanceId: String,
  target: String,
  artifactId: Option[String],
  executionMode: String,
  developmentDirectory: Option[String],
  subsystemName: Option[String],
  subsystemVersion: Option[String],
  runtimeVersion: String,
  startedAt: Instant,
  lastSeenAt: Instant,
  stoppedAt: Option[Instant]
)

final case class LocalServerEvidenceSnapshot(
  schema: String,
  entries: Vector[LocalServerEvidenceEntry]
)

object LocalServerEvidenceSnapshot {
  val Schema = "cncf.launcher.server-evidence.v1"

  given Encoder[LocalServerEvidenceEntry] = deriveEncoder
  given Decoder[LocalServerEvidenceEntry] = deriveDecoder
  given Encoder[LocalServerEvidenceSnapshot] = deriveEncoder
  given Decoder[LocalServerEvidenceSnapshot] = deriveDecoder
}

final class LocalServerEvidenceStore(paths: LauncherPaths) {
  import LocalServerEvidenceSnapshot.given

  def started(report: TextusControlCenterRegistrationReport, launcherkind: String): Unit =
    _update { entries =>
      val now = Instant.now()
      entries.filterNot(_.instanceId == report.instanceId) :+ LocalServerEvidenceEntry(
        launcherkind,
        report.instanceId,
        report.target,
        report.artifactId,
        report.executionMode,
        report.developmentDirectory,
        report.subsystemName,
        report.subsystemVersion,
        report.runtimeVersion,
        report.startedAt,
        now,
        None
      )
    }

  def alive(instanceid: String): Unit =
    _update(_touch(_, instanceid) { entry =>
      if (entry.stoppedAt.isEmpty) Some(entry.copy(lastSeenAt = Instant.now())) else None
    })

  def stopped(instanceid: String): Unit =
    _update(_touch(_, instanceid) { entry =>
      if (entry.stoppedAt.isEmpty) {
        val now = Instant.now()
        Some(entry.copy(lastSeenAt = now, stoppedAt = Some(now)))
      } else None
    })

  private def _update(f: Vector[LocalServerEvidenceEntry] => Vector[LocalServerEvidenceEntry]): Unit = LocalServerEvidenceStore.lock.synchronized {
    Files.createDirectories(paths.serverEvidence.getParent)
    val channel = FileChannel.open(paths.serverEvidence.resolveSibling("server-evidence.lock"), CREATE, WRITE)
    try {
      val lock = channel.lock()
      try {
        val snapshot = _load()
        val updated = LocalServerEvidenceSnapshot(LocalServerEvidenceSnapshot.Schema, _retain(f(snapshot.entries), Instant.now()))
        val temporary = Files.createTempFile(paths.serverEvidence.getParent, ".server-evidence-", ".json")
        try {
          Files.writeString(temporary, updated.asJson.noSpaces + "\n", StandardCharsets.UTF_8)
          Files.move(temporary, paths.serverEvidence, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
          Files.deleteIfExists(temporary)
        }
      } finally {
        lock.release()
      }
    } finally {
      channel.close()
    }
  }

  private def _load(): LocalServerEvidenceSnapshot =
    if (!Files.isRegularFile(paths.serverEvidence))
      LocalServerEvidenceSnapshot(LocalServerEvidenceSnapshot.Schema, Vector.empty)
    else
      decode[LocalServerEvidenceSnapshot](Files.readString(paths.serverEvidence, StandardCharsets.UTF_8)).toOption.
        filter(_.schema == LocalServerEvidenceSnapshot.Schema).
        getOrElse(_recover_malformed())

  private def _recover_malformed(): LocalServerEvidenceSnapshot = {
    val recovery = paths.serverEvidence.resolveSibling(s"server-evidence.recovery-${java.util.UUID.randomUUID().toString}.json")
    try Files.move(paths.serverEvidence, recovery, StandardCopyOption.ATOMIC_MOVE)
    catch {
      case _: AtomicMoveNotSupportedException => Files.move(paths.serverEvidence, recovery)
    }
    LocalServerEvidenceSnapshot(LocalServerEvidenceSnapshot.Schema, Vector.empty)
  }

  private def _touch(
    entries: Vector[LocalServerEvidenceEntry],
    instanceid: String
  )(f: LocalServerEvidenceEntry => Option[LocalServerEvidenceEntry]): Vector[LocalServerEvidenceEntry] =
    entries.find(_.instanceId == instanceid).flatMap(f) match {
      case Some(updated) => entries.filterNot(_.instanceId == instanceid) :+ updated
      case None => entries
    }

  private def _retain(entries: Vector[LocalServerEvidenceEntry], now: Instant): Vector[LocalServerEvidenceEntry] =
    entries.filter(entry => !entry.lastSeenAt.plus(LocalServerEvidenceStore.Retention).isBefore(now)).takeRight(LocalServerEvidenceStore.MaximumEntries)
}

object LocalServerEvidenceStore {
  val Retention: Duration = Duration.ofDays(30)
  val MaximumEntries = 512
  private val lock = new Object
}

trait LocalServerEvidenceSession {
  def close(): Unit
}

object LocalServerEvidenceSession {
  def start(
    paths: LauncherPaths,
    report: TextusControlCenterRegistrationReport,
    launcherKind: String,
    heartbeatInterval: Duration = Duration.ofSeconds(30)
  ): LocalServerEvidenceSession = {
    val store = LocalServerEvidenceStore(paths)
    store.started(report, launcherKind)
    val closed = AtomicBoolean(false)
    val executor = Executors.newSingleThreadScheduledExecutor { runnable =>
      val thread = Thread(runnable, "textus-local-server-evidence-heartbeat")
      thread.setDaemon(true)
      thread
    }
    executor.scheduleAtFixedRate(
      () => if (!closed.get) store.alive(report.instanceId),
      heartbeatInterval.toMillis,
      heartbeatInterval.toMillis,
      TimeUnit.MILLISECONDS
    )
    new LocalServerEvidenceSession {
      def close(): Unit =
        if (closed.compareAndSet(false, true)) {
          executor.shutdownNow()
          store.stopped(report.instanceId)
        }
    }
  }

  val noop: LocalServerEvidenceSession = new LocalServerEvidenceSession {
    def close(): Unit = ()
  }
}

# Local CAR Activation

status = active
scope = resolved local archive provenance and native runtime activation

`ResolvedArtifact.localArchive` records the exact regular archive selected from a
local repository as a normalized absolute path. The trailing optional field
defaults to `None`, preserving existing positional source constructors. This is
not a binary ABI guarantee. Artifact selectors remain artifact identities and
are not CNCF Component IDs.

Local metadata resolution retains the selected explicit version or metadata
latest version's archive. Catalog resolution retains the selected version's
authored `file` override, or its conventional archive path when no override is
present. Catalog runtime and dependency requirements, repository precedence,
missing-archive diagnostics, disabled-version handling, local-only snapshots,
and CAR/SAR/Auto ambiguity rules remain unchanged. Remote metadata and catalog
resolution retain `None`; this contract introduces no remote download or cache
behavior.

For a resolved CAR with `localArchive = Some(path)`, Textus invokes the existing
native runtime contract with exactly `--textus.component.file=<path>`, omitting
the artifact-name `--textus.component=` and `--textus.component.version=` options.
The CAR descriptor and ABI remain authoritative for native component identity.
The Launcher does not synthesize qualified Component IDs or override drivers.

CARs with `None` retain their component selector and optional version options.
SARs retain their subsystem selector, including when their resolved local
archive is available. Repository options, command mode, ordinary and passthrough
argument order and spelling are preserved. Registration and shared evidence
continue to report the artifact name and selected version; local archive paths
are not added to those records. Lifecycle ownership remains with Supervisor.

[LocalCarActivationSpec](../../src/test/scala/textus/launcher/LocalCarActivationSpec.scala)
specifies metadata explicit/latest paths, catalog override/default paths and
runtime requirements, precedence and missing/disabled catalog behavior, and
recorded public Launcher invocations for CAR and SAR. Generated artifact names,
versions and path variations require at least 50 successful cases and zero
discards. The `None` compatibility case checks the existing argument conversion
directly without a remote repository. Fixtures live only under `target/`.

Recording invocations do not establish real native archive loading, a second
Launcher JVM, server readiness, safe live/stopped evidence, or cleanup. Those
remain parent-owned native runtime and lifecycle acceptance checks.

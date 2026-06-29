# Textus Launcher

`textus` is the user-facing launcher for Textus/CNCF applications.
It keeps CNCF development commands separate from application operation:

```bash
textus textus-blog server
textus textus-blog client ...
textus textus-blog:0.1.0 command blog.post.search limit=10
textus runtime current
```

The launcher resolves a selected CNCF runtime and invokes
`org.goldenport.cncf.CncfMain` in the same JVM process. Textus launcher
configuration is read from:

- `~/.textus/config.yaml`
- ancestor `.textus/config.yaml` files, outermost first
- `$PWD/.textus/config.yaml`

Runtime cache and resolved classpath metadata are kept under `~/.cncf`.
Runtime version selection is driven by the Textus runtime catalog in the
warehouse repository:

```text
https://www.simplemodeling.org/repository/textus/runtime-catalog.yaml
```

This catalog is operational metadata. It is not sourced from SmartDox article
publication, so `recommended` and `disabled` changes are not coupled to article
deployment timing.

## CLI Installation

`textus install-cli` installs a user-facing command that delegates to a
packaged CAR/SAR artifact through `textus <artifact> command`. This is the operational
path for general users.

```bash
textus install-cli sanpomap textus-sanpomap:0.1.0 \
  --operation-prefix sanpomap.presentation \
  --file-param presentationDsl \
  --bin-dir ~/bin \
  --overwrite

sanpomap validate-presentation --presentationDsl xxx.yaml --format yaml
```

The installed command treats the first argument as an operation name and
combines it with the operation prefix. In the example above,
`validate-presentation` becomes
`sanpomap.presentation.validate-presentation`.

`--file-param <name>` marks parameters whose values may be file paths. If the
value exists as a file, the installed command reads the file and passes its
contents to the CNCF operation. Camel-case and kebab-case aliases are both
accepted, so `presentationDsl` also accepts `--presentation-dsl`.

## Configuration

`textus` reads standard launcher configuration files:

1. `~/.textus/config.yaml`
2. ancestor `.textus/config.yaml` files, outermost first
3. `$PWD/.textus/config.yaml`

The current directory file overrides inherited ancestor and home files. Use `--config <file>` for
an additional Textus launcher config file. Command-line options override both
standard config files and explicit launcher config files.

Textus launcher config is intentionally lightweight. It supports `yaml` /
`yml`, `properties` / `props`, and lightweight `conf` files with dotted keys.
JSON, XML, and full HOCON are runtime/application config formats, not launcher
config formats.

Example:

```yaml
runtime:
  version: recommended
  catalog:
    url: https://www.simplemodeling.org/repository/textus/runtime-catalog.yaml

repositories:
  car:
    - https://example.com/repository/car
  sar:
    - https://example.com/repository/sar
  maven:
    - https://example.com/repository/maven
```

Project repositories are searched before the standard SimpleModeling.org
repositories. The standard repositories remain enabled as fallback.
The launcher also searches the local CNCF repository before cache and public
repositories:

```text
~/.cncf/local/repository/car
~/.cncf/local/repository/sar
```

Use `sbt cozyPublishLocalCar` or `sbt cozyPublishLocalSar` while developing
dependency components. Those tasks write local CAR/SAR artifacts, catalogs, and
derived `maven-metadata.xml` under `~/.cncf/local`. This directory is
developer-owned local publish state. `~/.cncf/cache` is runtime-managed remote
artifact cache and is managed separately. Snapshot components are local-only by
default; missing snapshots should be published with `sbt cozyPublishLocalCar`.

`.cozy/config.yaml` controls build/publish operation defaults.
`.textus/config.yaml` controls the user-facing Textus launcher. The `cncf`
developer launcher uses `.cncf/launcher.yaml`, not `.cncf/config.yaml`.
`.cncf/config.yaml` and `.textus/config.yaml` may also be consumed by the CNCF
runtime after the launcher starts it, depending on runtime configuration
resolution. `project.yaml` describes artifact metadata and runtime
compatibility.

## Runtime Management

Launcher/runtime management is intentionally named `runtime`, not `admin`, to
avoid confusion with CNCF Application Admin, System Admin, and component admin
surfaces.

```bash
textus runtime current
textus runtime refresh
textus runtime remote list
textus runtime catalog show
textus runtime channels
textus runtime use latest
textus runtime use newest
textus runtime use 0.5.0-SNAPSHOT --project
textus runtime cache status
textus runtime config show
```

`textus runtime use <version>` writes project scope when the current directory
has `.textus/`; otherwise it writes the global launcher version. Use `--project`
or `--global` to force the target. A fresh install with no version file uses
`recommended` from the runtime catalog.

Runtime selector terms are:

- `recommended`: operator-selected default runtime from the catalog.
- `latest`: alias of `latest-stable`; the newest stable runtime in the catalog.
- `latest-stable`: newest stable runtime in the catalog.
- `latest-snapshot`: newest snapshot runtime in the catalog.
- `newest`: newest enabled runtime across all catalog channels.

When a component catalog declares `runtime.cncf` compatibility, `textus` uses
`current-compatible` selection by default: the current runtime selector
normally resolves to `recommended`, and it is used when it satisfies all
component requirements. Use `--runtime-selection=tested-latest`,
`--runtime-selection=latest`, or `--runtime-selection=newest` to choose a
different compatible-runtime policy.

The runtime selector and compatibility-selection semantics are intentionally
duplicated in the `textus` and `cncf` launchers instead of being factored into a
shared launcher-core library. These launchers are small, separately distributed
entrypoints and are expected to stabilize. When changing `recommended`,
`latest`, `newest`, or `runtime.cncf` compatibility behavior, update both
launchers and their tests together.

## Artifact Version Syntax

Use `artifact:version` when selecting an explicit CAR/SAR version:

```bash
textus textus-blog:0.1.0 server
textus textus-blog:0.1.0 command blog.post.search limit=10
```

The older `artifact@version` spelling remains accepted for compatibility, but
new documentation and scripts should use `artifact:version`.

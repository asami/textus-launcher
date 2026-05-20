# Textus Launcher

`textus` is the user-facing launcher for Textus/CNCF applications.
It keeps CNCF development commands separate from application operation:

```bash
textus server textus-blog
textus client textus-blog ...
textus command textus-blog:0.1.0 blog.post.search limit=10
textus runtime current
```

The launcher resolves a selected CNCF runtime and invokes
`org.goldenport.cncf.CncfMain` in the same JVM process. Runtime and project
configuration is read only from:

- `~/.textus/config.yaml`
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

## Configuration

`textus` reads only two configuration files:

1. `~/.textus/config.yaml`
2. `$PWD/.textus/config.yaml`

The current directory file overrides the home file. Command-line options
override both.

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
textus server textus-blog:0.1.0
textus command textus-blog:0.1.0 blog.post.search limit=10
```

The older `artifact@version` spelling remains accepted for compatibility, but
new documentation and scripts should use `artifact:version`.

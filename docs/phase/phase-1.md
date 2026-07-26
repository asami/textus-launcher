# Phase 1: Published Skill Bundle Installation

Status: planned

## Goal

Allow a user to explicitly install, inspect, update, and remove a Codex skill
bundle declared by a resolved CAR without requiring a source checkout.

## Command Surface

```text
textus skill list <artifact>
textus skill install <artifact> [--scope user|project] [--configure-mcp]
textus skill status <bundle-or-artifact>
textus skill update <bundle-or-artifact> [--configure-mcp]
textus skill uninstall <bundle-or-artifact>
```

`user` installs under the configured Codex home, normally `~/.codex/skills`.
`project` installs under the selected project's `.codex/skills`. `textus`
defaults to `user`; no command may overwrite an existing skill, configuration,
or a different bundle without an explicit replacement option.

## Scope

- resolve a CAR from configured repositories, `~/.cncf/local`, or cache using
  the existing artifact-resolution policy;
- read and validate the CNCF `SkillBundleManifest` and every declared file
  digest before changing an installed scope;
- stage an installation, activate it only after complete validation, retain
  enough installed provenance for `status`, `update`, and `uninstall`, and
  recover safely from an interrupted installation;
- display the effective bundle, CAR, skill, compatibility, and MCP
  requirements before installation;
- offer, but never silently perform, a narrowly scoped MCP configuration merge
  through `--configure-mcp`; and
- report that a new Codex task or reload may be required for skill/MCP
  discovery.

## Non-goals

- downloading or executing arbitrary scripts from a bundle;
- installing transitive skill dependencies without user confirmation;
- changing a skill's contents to fit a particular Codex task;
- starting a CAR server, invoking MCP tools, or performing an AI-cost-bearing
  action as part of installation; and
- supporting development-directory source bundles; that is CNCF Launcher's
  matching workflow.

## Dependencies

- CNCF stable `SkillBundleManifest` model, archive location, digest, and
  compatibility rules;
- Cozy CAR packaging and manifest validation; and
- Codex installation-layout and TOML-merge adapter isolated behind a launcher
  port so user configuration stays client-owned.

## Acceptance

- A valid published or locally published CAR can be listed and installed into
  an empty user or project Codex scope.
- Invalid manifests, missing files, digest mismatch, unsupported Codex
  compatibility, name collision, and denied MCP merge fail before activation.
- `status`, `update`, and `uninstall` identify the installed CAR and bundle
  version and preserve unrelated skills and MCP settings.
- Deterministic specifications cover repository/local/cache resolution,
  staging, upgrade, rollback, scope, and configuration-merge refusal.

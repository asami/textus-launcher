# Textus Launcher Skill Bundle Installation Direction — 2026-07-21

## Context

`textus install-cli` already gives a published CAR an explicit user-level
installation route by creating a command wrapper under `~/bin`. Codex skills
need the same discoverable installation experience, but are files and policy
instructions for Codex rather than executable shell wrappers.

## Direction

Textus Launcher will add a generic `textus skill` command family for a CAR's
declared skill bundle. Its target is normally `~/.codex/skills`, with an
explicit project-scope alternative. It resolves a published, locally
published, or cached CAR; it does not require the component's source tree.

The launcher owns local installation safety: manifest/digest validation,
staging, collision handling, installed provenance, update, removal, and
explicit MCP configuration merge. It must not silently overwrite user-owned
Codex configuration, install dependencies, execute bundle scripts, start a
server, or call cost-bearing AI/MCP operations.

The bundle contract belongs to CNCF and packaging belongs to Cozy. This keeps
the installer generic: CBD Support is a first consumer, not a special command
path. Development-tree installation is delegated to CNCF Launcher.

## Documentation Result

The planned work is defined as Textus Launcher Phase 1 in
`docs/strategy/textus-launcher-development-strategy.md` and
`docs/phase/phase-1.md`.

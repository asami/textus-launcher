# Textus Launcher Development Strategy

Status: active

## Purpose

`textus` is the user/operator launcher for published Textus applications and
CAR/SAR artifacts. It provides an explicit, versioned installation path for
user-facing commands and Codex skills without making either installation a
runtime side effect of artifact resolution.

## Direction

Textus Launcher resolves a published, locally published, or cached CAR and
installs the CAR's declared Codex skill bundle into the selected Codex scope.
It does not define CAR skill content, interpret a skill's domain policy, or
make a remote MCP server authoritative over the local Codex installation.

The common `SkillBundleManifest` contract is owned by CNCF. Cozy produces and
packages the manifest and bundle; Textus Launcher validates, stages, installs,
reports, updates, and removes the selected installed bundle.

## Roadmap

### Phase 1: Published Skill Bundle Installation

Status: planned.

Add `textus skill list|install|status|update|uninstall` for versioned skill
bundles declared by resolved CAR artifacts. Install into either the user Codex
home or an explicitly selected project scope, validate manifest and file
digests before activation, and preserve existing user configuration unless an
explicit option admits an MCP configuration merge.

The matching development-worktree workflow is owned by CNCF Launcher Phase 1.

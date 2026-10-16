# Phase 3: Docker Control Center Integration

Status: PLANNED
Planned: 2026-10-16
Upstream: textus-control-center Phase 8, goldenport-cncf Phase 113

## Goal

Validate and minimally extend the canonical Textus launcher path for CAR execution inside the Docker Compose operational profile.

## Scope

- Preserve the existing registration, heartbeat and deregistration protocol.
- Support a Control Center registration endpoint addressed by Compose service DNS.
- Keep the registered Subsystem base URL independent from the internal registration endpoint.
- Add only bounded execution-context metadata required by the accepted Control Center Phase 8 contract.
- Preserve common Launcher evidence and best-effort Supervisor notification semantics from Control Center Phase 4.
- Validate normal termination and restart/re-registration under Docker execution.

## Boundaries

- Do not make Textus Launcher a Docker API client.
- Do not control container lifecycle from the Launcher.
- Do not expose container secrets or unrestricted Docker metadata.
- Docker lifecycle authority belongs to Textus Supervisor.

## Acceptance

A CAR started through the canonical Textus launcher path inside the OPS Compose network registers, heartbeats and deregisters correctly, retains the accepted evidence semantics, and can be correlated with its Docker execution boundary without changing application health semantics.

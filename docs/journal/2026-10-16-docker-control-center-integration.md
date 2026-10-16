# Docker execution and Control Center integration — 2026-10-16

## Decision

`textus-launcher` running inside a CAR container keeps the existing Textus Control Center integration contract. Docker does not introduce a second registration protocol.

The launcher continues to own invocation identity/evidence and sends registration, heartbeat and deregistration to Control Center. In the Mac mini OPS Docker Compose profile, the Control Center endpoint may use the Compose service DNS name on the shared internal network.

The registered Subsystem `baseUrl` is a separate concept: it remains the canonical operator-reachable URL and may be published through Cloudflare Tunnel.

## Execution context

A later protocol revision may carry minimal execution-context metadata sufficient to distinguish Docker execution from native execution and associate the instance with an execution boundary/Supervisor. Keep this metadata stable and bounded.

The launcher must not:

- inspect the Docker daemon as part of normal registration;
- become a Docker lifecycle controller;
- expose container secrets or unrestricted Docker metadata;
- infer Subsystem health from container state.

Docker lifecycle control belongs to the Textus Supervisor execution provider. Control Center Phase 8 owns the cross-component integration.

## Future host execution

The initial Mac mini OPS implementation is Docker-only. A native/host Supervisor is planned, so launcher semantics must remain execution-environment-neutral.

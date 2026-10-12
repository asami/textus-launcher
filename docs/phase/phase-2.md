# Phase 2: macOS Control Center Installation

## Goal

Provide the Textus installation path from a textus-control-center CAR to a running macOS Menu Bar application and Control Center connection.

## Scope

- Consume cncf-launcher platform-Subcomponent resolution.
- Install the selected Textus Control Center .app.
- Support login-time startup.
- Start/connect to the Control Center service.
- Support bundled and separate-CAR Menu Bar distribution.
- Keep application management logic in Control Center rather than in the launcher.

## Service Bus migration direction

- Preserve existing dedicated launcher management/storage as the authoritative current-state source initially.
- Add meaningful lifecycle event publication to CNCF Service Bus in parallel; do not replace storage in the first integration.
- Use the journal for Control Center operational history/timelines as it matures.
- Only after stable operation, migrate duplicated historical-record responsibilities toward Service Bus Journal.
- Do not reconstruct launcher current state from the journal; Event Sourcing is not a goal.

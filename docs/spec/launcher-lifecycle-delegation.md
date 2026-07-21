# Standalone Lifecycle Delegation

status = in_progress
scope = Textus Control Center Phase 4

`textus-launcher` does not create a second lifecycle owner. Repository-CAR
lifecycle requests use the same authenticated loopback CNCF launcher supervisor
contract as development-directory requests. The supervisor owns request,
idempotency, child-process, and instance correlation state below `~/.cncf/`.

The launcher must preserve safe `accepted`, `running`, `stopped`, `rejected`,
`failed`, and `timed-out` results, and must not discover or signal arbitrary
processes to emulate ownership.

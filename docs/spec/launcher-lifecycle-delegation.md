# Provisional Launcher Lifecycle Delegation

status = superseded-by-phase-4-reopen
scope = Textus Control Center Phase 4 transition baseline

`textus-launcher` does not create lifecycle ownership. The former loopback CNCF
launcher-supervisor delegation is a transition baseline, superseded by
`textus-supervisor` as the lifecycle authority. In standalone operation Control
Center embeds that supervisor; future distributed placement preserves the same
authenticated command/result contract. Textus Launcher retains common evidence
and bounded best-effort notification only, so Control Center availability never
blocks `textus <artifact> server`.

The launcher must preserve safe `accepted`, `running`, `stopped`, `rejected`,
`failed`, and `timed-out` results, and must not discover or signal arbitrary
processes to emulate ownership.

The adapter submits and looks up the same `/v1/lifecycle-requests` loopback
protocol with a configured bearer credential and bounded timeout. It accepts
only `http` loopback endpoints (`127.0.0.1`, `::1`, or `localhost`), returns the
supervisor's safe JSON result unchanged, and makes no process-management call.
Malformed request identity, credentials, timeout, or endpoint are reported as
safe local errors before any network request.

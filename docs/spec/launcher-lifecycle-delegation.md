# Standalone Lifecycle Delegation

status = implemented
scope = Textus Control Center Phase 4

`textus-launcher` does not create a second lifecycle owner. Repository-CAR
lifecycle requests use the same authenticated loopback CNCF launcher supervisor
contract as development-directory requests. The supervisor owns request,
idempotency, child-process, and instance correlation state below `~/.cncf/`.

The launcher must preserve safe `accepted`, `running`, `stopped`, `rejected`,
`failed`, and `timed-out` results, and must not discover or signal arbitrary
processes to emulate ownership.

The adapter submits and looks up the same `/v1/lifecycle-requests` loopback
protocol with a configured bearer credential and bounded timeout. It accepts
only `http` loopback endpoints (`127.0.0.1`, `::1`, or `localhost`), returns the
supervisor's safe JSON result unchanged, and makes no process-management call.
Malformed request identity, credentials, timeout, or endpoint are reported as
safe local errors before any network request.

# Launcher Observation and Lifecycle Delegation

status = active
scope = canonical Textus observation and historical transition adapter

`textus <artifact> server` retains its canonical artifact and runtime resolution.
Textus Launcher writes the shared versioned evidence store with the common
lock and retention policy before any best-effort notification. It owns
invocation facts; `textus-supervisor` owns lifecycle execution and durable
ownership. Control Center invokes that provider through its standard Supervisor
socket and protected Operations. No foreground Launcher daemon is required.

## Existing Notification Surface

Notification uses the existing protocol-version-1 Control Center GET Operations
`register-subsystem`, `heartbeat-subsystem`, and `deregister-subsystem`, with the
existing credential, launcher kind, and invocation identity. No new Supervisor
Operation or transport is introduced.

Reporter start returns without performing HTTP on the canonical startup caller.
One daemon scheduled executor checks `textus.server.bound-snapshot` at intervals
of at most 50 ms, even with a configured public base. Only coherent `v1` HTTP(S)
origin and optional safe `/web` or `/web/...` path readiness can activate it.
Legacy bound-base/application properties cannot activate or supply pieces of
readiness. A valid configured public origin takes precedence after readiness;
the selected path composes a same-origin application URL, omitted when absent.
The captured snapshot remains fixed for registration, heartbeat, and terminal
notification.

Initial registration sends `starting`; failures before acknowledgment retry
only at later configured heartbeat intervals. Acknowledgment remains true after
a heartbeat failure, so later intervals continue `running` without another
`starting` report. All requests serialize on the existing worker and use fixed
delay without catch-up bursts. Positive submillisecond intervals round to 1 ms.

Connect and read timeout settings each round positive submillisecond values to
1 ms and cap at 5000 ms. No redirect is followed, so an attempt has at most one
connect and one read phase, at most 10 seconds combined at the caps. Warnings
omit credentials, query payloads, and unrestricted endpoint details.

## Terminal Cleanup

The shutdown hook and `finally` share cleanup that closes durable evidence
first, then best-effort registration. Each nonfatal failure is isolated and
the original runtime return or exception is preserved. Idempotent registration
close cancels periodic work without waiting for an in-flight initial request.
Before activation it emits no HTTP. An acknowledged register receives at most
one `deregister-subsystem`/`stopped` attempt; if acknowledgment races close, the
existing daemon worker performs that terminal attempt after bounded completion.
Close creates no executor or retry loop, and no later register or heartbeat is
scheduled. Notification cannot prevent JVM exit or require Control Center
availability for durable evidence.

CNCF Launcher compatibility commands `launcher supervisor serve` and
`launcher lifecycle ensure|submit|lookup` are retired diagnostics with code
`launcher-lifecycle-authority-retired`. Lifecycle users use protected Control
Center Operations backed by `textus-supervisor`.

## Historical Transition Adapter (Retired Authority)

The earlier loopback CNCF launcher-supervisor delegation was a prototype,
superseded by `textus-supervisor`. The following protocol description is
historical internal coverage, not a current Launcher authority or prerequisite.

The prototype adapter preserved safe `accepted`, `running`, `stopped`, `rejected`,
`failed`, and `timed-out` results and did not discover or signal arbitrary
processes to emulate ownership.

The prototype adapter submitted and looked up the same `/v1/lifecycle-requests` loopback
protocol with a configured bearer credential and bounded timeout. It accepted
only `http` loopback endpoints (`127.0.0.1`, `::1`, or `localhost`), returned the
supervisor's safe JSON result unchanged, and made no process-management call.
Malformed request identity, credentials, timeout, or endpoint were reported as
safe local errors before any network request.

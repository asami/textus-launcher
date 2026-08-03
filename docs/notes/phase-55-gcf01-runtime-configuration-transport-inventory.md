# Phase 55 GCF-01 Textus Launcher Configuration Transport Inventory

Date: 2026-08-02

Status: frozen implementation inventory

`textus-launcher` reads launcher-local files and environment variables and
delegates to a selected runtime. It is a String transport boundary, not a
configuration resolver. Its existing launcher aliases and development-runtime
selection remain outside the semantic parameter catalog unless GCF-09 explicitly
admits a consumer.

For Phase 55 runtime configuration, the launcher must preserve external input
and provenance, use only explicit boundary codecs, and never choose source
rank, target specificity, or a winning binding. The canonical external namespace
is `textus.*`; `textus.runtime.*`, `cncf.*`, and `cncf.runtime.*` are accepted
only while decoding legacy input. Coexistence of canonical and alias spellings
for one parameter/target in one physical source is rejected structurally.

GCF-08 provides the round-trip and non-collision codec proof. GCF-09 removes
admitted internal alias consumers. This record does not modify launcher-local
configuration behavior or publish a new compatibility promise.

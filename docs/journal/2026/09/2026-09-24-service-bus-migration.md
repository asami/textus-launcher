# Service Bus Migration

Date: 2026-09-24

Decision: Service Bus adoption is additive first. Existing Textus launcher management data remains authoritative. Lifecycle events are added in parallel and used by Control Center. Once journal operation is proven reliable, historical information can be consolidated into Service Bus Journal while current-state management remains in the launcher domain.

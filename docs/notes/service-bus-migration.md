# Service Bus Migration

textus-launcher keeps its existing dedicated management/storage area as the authoritative current-state source while CNCF Service Bus integration is introduced.

## Staged migration

1. Existing dedicated storage remains unchanged and authoritative.
2. Publish meaningful Textus/launcher lifecycle events in parallel to Service Bus, using authoritative journaling where appropriate.
3. Let Control Center increasingly consume the journal for operational timelines and history.
4. After the event contract and SQLite/PostgreSQL journal operation are proven stable, remove duplicated historical-record responsibilities from dedicated storage where beneficial.

Current installed/configured state remains normal registry/state data; it is not reconstructed from events. This migration explicitly avoids Event Sourcing.

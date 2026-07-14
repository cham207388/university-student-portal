# ADR 0003: DynamoDB table design

## Context

The API needs direct entity lookup, alternate-identifier uniqueness, department and instructor relationships, status
queries, cursor pagination, and concurrency-safe enrollment without relational joins or foreign keys.

## Decision

Use one composite-key table with four sparse GSIs, unique-claim items, an authoritative enrollment item, two
relationship copies per enrollment, an active-pair claim, and one course capacity-control item. Terraform is the sole
owner of table creation. The complete key mapping is recorded in `docs/dynamodb-table-design.md`.

## Alternatives considered

- One table per entity: familiar, but enrollment relationship reads and integrity transactions become less instructive
  and require more cross-table operations.
- A minimal single table with metadata only: cannot satisfy alternate indexes, relationship collections, or bounded
  enrollment operations without scans.
- More GSIs for every filter combination: improves query breadth but creates high write/storage amplification and still
  cannot provide arbitrary SQL-like combinations.

## Consequences

Core endpoints use key operations rather than scans. Enrollment writes amplify into several items and must be
transactional. GSI list reads are eventually consistent. Unsupported search/filter combinations are explicit API
errors. Migration and reconciliation must distinguish physical copies from logical entities.

## Risks

The course capacity item can become hot under extreme contention. Denormalized relationship copies can diverge only if
transaction ownership is bypassed or corrupted externally. Unique claims and referenced metadata need careful cleanup
on updates and deletes.

## Validation approach

Terraform validation and planning must remain warning-free. Integration tests will exercise every GSI, conditional
claim, enrollment transaction, cursor, rollback path, and reconciliation rule against LocalStack Testcontainers.

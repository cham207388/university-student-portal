# DynamoDB limitations and operational tradeoffs

## Query limitations

- Collections support only documented partition/sort-key access paths. Course title search, arbitrary last-name search,
  credit ranges, arbitrary multi-filter combinations, dynamic sorting, offsets, exact totals, and total pages are not
  efficient and return `400` when requested through the API.
- GSIs are eventually consistent. A just-written entity can be briefly absent from an alternate-key or relationship
  collection even though its primary-key read succeeds.
- Pagination is forward-only through an opaque `LastEvaluatedKey`. Random page access is unavailable.
- DynamoDB applies its evaluated-item limit before filters; core endpoints avoid filter expressions and scans so page
  behavior remains predictable.

## Integrity limitations and mitigations

DynamoDB provides no foreign keys, cascades, unique constraints, or joins. The application mitigates this with:

- strong parent reads before commands;
- transaction conditions on parent existence, status, version, and capacity;
- deterministic alternate-key claim items for uniqueness;
- authoritative parent dependency and enrollment-history counters for delete races;
- deterministic active-enrollment locks;
- durable Student/Course relationship edges;
- optimistic versions on mutable domain records.

These rules protect writes that use the application. Out-of-band table edits can violate invariants and require explicit
reconciliation. GSIs remain diagnostic/read paths and must never replace authoritative conditions.

## Transaction constraints

AWS DynamoDB transactions permit at most 100 actions and 4 MB of aggregate item data. An item cannot be targeted by two
actions in the same transaction. Transactions have higher latency and consumed-capacity cost than individual operations.
The current command transactions are deliberately small, but future bulk operations must chunk work and cannot claim
cross-chunk atomicity.

Cancellation reasons do not expose a stable business taxonomy for every failure. The application translates conditional
cancellation into a safe `409` contract and does not leak raw AWS messages. Retry is appropriate only after rereading
state; blindly retrying a stale optimistic command is incorrect.

## Denormalization costs

Catalog keys, normalized sort keys, counters, active locks, claims, and relationship edges duplicate logical facts. Every
write path must maintain the relevant copies atomically. New access patterns can require a new attribute, GSI, backfill,
Terraform rollout, and cursor identity. Item-size and write-amplification costs grow with denormalization.

Counters are authoritative guards, not analytics. Operational reconciliation should compare them with paginated source
records and repair only through an audited procedure.

## Capacity and hot partitions

PAY_PER_REQUEST removes capacity planning for this local phase but does not remove throttling, hot-key, or cost risk.
Status and catalog indexes concentrate traffic into low-cardinality partitions and are suitable for this bounded learning
dataset, not unlimited production scale. A production design may need time buckets, sharding, quotas, alarms, and retry
with jitter.

Course capacity and active-pair correctness are transactionally enforced. High contention on a popular course can still
increase conflicts and latency even though it cannot oversubscribe the course.

## LocalStack differences

LocalStack is an emulator, not a proof of identical AWS control-plane behavior. With LocalStack 4.14 and AWS provider
6.54, GSIs added with `UpdateTable` can report explicit unlimited on-demand metadata and provider waiters can wait for
warm-throughput state that the emulator omits. The verified local recovery is documented in
`dynamodb-development-data.md`. Never apply destructive emulator recovery procedures to AWS or non-disposable data.

Testcontainers proves isolated runtime behavior without a developer-managed instance. Terraform validation and a real
LocalStack convergence plan separately verify infrastructure. AWS staging validation will still be required before a
production release.

## Observability and privacy

The application emits correlation IDs, method/path/status/duration request logs, a secret-free DynamoDB startup summary,
and a six-table health contribution. Health details are hidden over HTTP by default. Logs intentionally exclude query
values, request/response bodies, student profiles, passwords, credentials, and LocalStack tokens.

Actuator request observations provide HTTP timing. Fine-grained DynamoDB operation metrics and production alarms are a
future operational concern; adding them must avoid high-cardinality IDs and personal data.

## Backup, recovery, and reconciliation

Local development data is deterministic and can be reseeded. A table must not be recreated independently while its
cross-table counters survive unless those counters are reconciled first. LocalStack backup/restore support can differ from
AWS, as observed for GSI metadata during the convergence recovery.

A production runbook must define point-in-time recovery, retention, restore drills, per-table reconciliation, rejected
record handling, and an outage window for cross-table restoration. Restoring only one table can leave logically invalid
relationships even when every restored item is structurally valid.

## PostgreSQL migration changes

The relational implementation will replace claim/lock/edge items and dependency counters with unique constraints,
foreign keys, join tables, transactional row updates, and database-enforced relationships. It can support broader search,
sorting, offsets, and totals, but must retain the shared domain rules, IDs, versions, API error semantics, and capacity
guarantees. Migration must:

1. read all six tables with independent resumable checkpoints;
2. distinguish logical entities from internal typed records;
3. validate references, enums, timestamps, versions, and legacy fields;
4. load in referential order and reconstruct relational joins;
5. compare source/target counts and relationship/capacity invariants;
6. report rejects without silent loss;
7. switch datasource profiles only after reconciliation passes.

DynamoDB cursors, GSI lag assumptions, and LocalStack endpoints are not portable to PostgreSQL.

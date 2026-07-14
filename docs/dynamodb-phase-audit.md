# Final DynamoDB phase audit

Audit date: 2026-07-14

## Decision

The DynamoDB datasource checkpoint is **complete and ready** for the `dynamodb-complete` tag. The implementation,
automated tests, live LocalStack workflow, Terraform convergence, API contract, observability, and documentation gates
all pass. No known DynamoDB-phase blocker remains.

## Passing gates

| Gate | Evidence |
| --- | --- |
| Java/Gradle build | `./gradlew clean check` passed on Java 25 / Gradle 9.5.1 |
| Automated tests | 39 unit/MVC tests and 12 LocalStack integration tests passed |
| DynamoDB persistence | Six domain tables, explicit Enhanced Client records, conditional writes, optimistic versions, transactional uniqueness claims, counters, locks, and relationship edges |
| Query behavior | Exact alternate-key GSI reads, cursor-bound collection GSI queries, strict unknown-parameter rejection, and no core endpoint scans |
| Enrollment concurrency | Duplicate-active, rollback, capacity, transition, lock lifecycle, and concurrent last-seat tests pass |
| Referential deletion | Authoritative dependency and enrollment-history counters close concurrent create/delete races |
| Derived relationships | Deduplicated edges, bounded strong-consistency batch hydration, reverse lookup, and opaque cursor tests pass |
| OpenAPI | Swagger UI at `/swagger-ui.html`; JSON at `/v3/api-docs`; 22 resource paths document schemas, errors, validation, and DynamoDB cursor semantics |
| Observability | Correlation-ID propagation/MDC, safe structured request logs, safe startup summary, and six-table DynamoDB health contributor |
| REST-to-DynamoDB test | Full Spring web context creates and reads data through controllers against Testcontainers LocalStack and checks health/OpenAPI |
| Seed and live API | Idempotent transactional seed plus the 14-request `scripts/dynamodb-api-smoke.sh` workflow passed on port 8080 |
| Compose and artifacts | `docker compose config --quiet`, shell syntax, `jq empty postman.json`, and `git diff --check` passed |
| Terraform | Recursive formatting, validation, real-state no-change plan, disposable six-table create/no-change/destroy cycle all passed |
| Secrets | No committed LocalStack token value found; logs and health failures omit credentials and raw exception details |
| Documentation | Architecture, item/access-path/enrollment diagrams, limitations, migration impacts, operations, README, and progress records are current |

## Closed blockers

The previous audit's four blockers are resolved:

1. Springdoc generates the OpenAPI contract and Swagger UI, with automated contract assertions.
2. DynamoDB health, correlation IDs, MDC propagation, safe request logging, and a safe startup summary are implemented.
3. A full application-context integration test exercises the REST boundary against LocalStack-backed tables.
4. `architecture-dynamodb.md` and `dynamodb-limitations.md` consolidate the required diagrams, limits, operational
   tradeoffs, reconciliation concerns, and PostgreSQL migration implications.

## Accepted implementation characteristics

- DynamoDB cannot enforce foreign keys natively. Application transactions, uniqueness claims, dependency counters,
  history counters, and active-enrollment locks provide the required integrity within DynamoDB transaction limits.
- Collection GSIs are eventually consistent. Authoritative parent counters and transactional writes protect destructive
  invariants; read views may briefly lag after a write.
- Opaque cursors are deliberately bound to their table, index, partition, and filters and are not portable between
  queries or storage engines.
- The Testcontainers schema mirrors Terraform and asserts table/index parity. It remains maintenance-sensitive, so a
  schema change must update both Terraform and the integration fixture in the same checkpoint.
- LocalStack is a development emulator, not evidence of identical production latency, throttling, IAM, backup, or
  failure behavior. These limits are explicit in `dynamodb-limitations.md`.

## Release result

The six intended LocalStack tables remain after the disposable audit tables were destroyed. The real Terraform state
has no drift. The checkpoint may be committed and tagged `dynamodb-complete`; PostgreSQL implementation and migration
work can begin from this stable boundary.

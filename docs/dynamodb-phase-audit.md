# Final DynamoDB phase audit

Audit date: 2026-07-14

## Decision

The DynamoDB implementation is functional but is **not ready** for the `dynamodb-complete` tag. Core domain,
persistence, concurrency, API, seed, live LocalStack, and Terraform convergence workflows pass. The tag remains blocked
by explicit prompt requirements that are not implemented.

## Passing gates

| Gate | Evidence |
| --- | --- |
| Java/Gradle build | `./gradlew clean check` passed on Java 25 / Gradle 9.5.1 |
| Automated tests | 35 unit/MVC tests and 11 LocalStack integration tests passed |
| DynamoDB persistence | Six domain tables, explicit Enhanced Client records, conditional writes, versions, transactions, and typed internal records |
| Query behavior | Cursor-bound GSI queries; no core endpoint scan calls found |
| Enrollment concurrency | Duplicate-active, rollback, capacity, transition, and concurrent last-seat tests pass |
| Referential deletion | Authoritative dependency/enrollment counters and concurrent create/delete tests pass |
| Derived relationships | Deduplicated edges, bounded batch hydration, reverse lookup, and cursor tests pass |
| Seed workflow | Explicit, disabled-by-default, idempotent seed with required cardinalities/statuses/full course |
| Live API | 12-request `scripts/dynamodb-api-smoke.sh` workflow passed against port 8080 |
| Compose and artifacts | `docker compose config --quiet`, `bash -n`, `jq empty postman.json`, and `git diff --check` passed |
| Terraform syntax | Recursive formatting and `terraform validate` passed |
| Terraform convergence | Clean-state reproduction and recovered real state both produced exit `0` no-change plans |
| Secrets | No committed LocalStack token value found; documented placeholders only |

## Tag blockers

### 1. OpenAPI is absent

The prompt requires Swagger UI, OpenAPI JSON, endpoint descriptions, schemas, validation constraints, errors,
pagination documentation, examples, and DynamoDB cursor guidance. No OpenAPI dependency, configuration, annotations, or
generated-document tests exist.

### 2. Required DynamoDB observability is incomplete

Actuator health exists, but there is no DynamoDB-specific health contributor, correlation-ID filter/MDC propagation,
or safe startup configuration summary. The prompt explicitly requires these capabilities.

### 3. Automated REST-to-DynamoDB coverage is missing

MVC slices use mocked services and persistence tests call adapters directly. The live shell workflow passes, but the
prompt requires REST endpoints in the automated LocalStack integration suite. Add a Spring Boot HTTP/MockMvc integration
test backed by the Testcontainers table set.

### 4. DynamoDB documentation checkpoint is incomplete

The required `architecture-dynamodb.md` and `dynamodb-limitations.md` files do not exist. Existing documents lack the
required Mermaid diagrams for item layout, main access patterns, and transactional enrollment flow. Transaction limits,
repair/reconciliation behavior, operational tradeoffs, and migration changes need one consolidated checkpoint.

## Additional quality gaps

- The fallback `Exception` handler is appropriate as a last boundary, but structured contextual request logging is not
  implemented.
- The Testcontainers schema is manually mirrored rather than derived from Terraform; parity is tested by assertions but
  remains maintenance-sensitive.
- The root README intentionally defers PostgreSQL/migration sections, but the DynamoDB checkpoint must link the new
  OpenAPI and limitations material once implemented.

## Required remediation order

1. Add OpenAPI/Swagger and contract tests.
2. Add correlation IDs, DynamoDB health, and safe startup logging with focused tests.
3. Add automated controller-to-LocalStack workflow coverage.
4. Complete the DynamoDB architecture/limitations documents and required Mermaid diagrams.
5. Rerun all automated, Terraform, clean-deploy, live HTTP, documentation, and secret gates.
6. Commit the completed checkpoint and create `dynamodb-complete` only if every blocker is closed.

# Progress Report

## Completed Tasks

### LocalStack RDS Terraform convergence

- Separated the requested PostgreSQL listener port from LocalStack's host-mapped edge port and made the local secret use
  the stable `localhost.localstack.cloud:4510` endpoint.
- Ignored LocalStack's emulator-only RDS port readback so Terraform no longer plans a perpetual `4510 -> 5432` update
  that causes the AWS provider to report an inconsistent sensitive `secret_string` during apply.
- Corrected the example PostgreSQL password to match the Terraform and Spring local defaults.

Verification results:

- `terraform fmt -check -recursive infrastructure/local`: successful.
- `terraform -chdir=infrastructure/local validate`: successful.
- Refreshed plan contains only the interrupted Secrets Manager version create; no RDS modification remains.

### PostgreSQL live datasource parity

- Replaced DynamoDB-specific controller query dependencies with datasource-neutral collection and relationship ports,
  retaining the DynamoDB implementations as source-specific subinterfaces for migration.
- Added PostgreSQL catalog, filter, date-range, and deduplicated relationship adapters with stable UUID ordering and
  opaque cursors bound to the exact query identity.
- Made enrollment creation and transitions lock the Course row, enforce capacity and active uniqueness, maintain
  occupied seats, release seats on drop, and serialize concurrent last-seat attempts.
- Applied expected-version checks consistently across PostgreSQL updates and deletes, mapped database/optimistic
  conflicts to RFC 9457 conflict responses, and preserved source audit timestamps, versions, and complete Profile fields.
- Added a Flyway migration for the previously omitted Profile address fields and enabled strict query-parameter
  enforcement for PostgreSQL controllers.
- Added a full `test-postgres` application/API Testcontainers suite covering startup, controllers, pagination, strict
  query contracts, filters, relationships, capacity release/reuse, concurrent enrollment, audit preservation, and stale
  mutation rejection.

Verification results:

- Targeted unit/MVC and PostgreSQL integration suites: successful.
- `./gradlew clean check`: successful; unit/MVC, DynamoDB LocalStack, and PostgreSQL Testcontainers suites passed.
- `git diff --check`: successful.

### Durable migration lifecycle tracking

- Added a second Flyway migration for `migration_runs`, `migration_checkpoints`, and `migration_errors`, including status,
  configuration, timestamps, per-entity counters, cursors, and safe terminal error summaries.
- Added a migration-profile JDBC tracker and wired the runner to record each run, completed entity stage, skipped rows,
  successful completion, and terminal failure.
- Extended PostgreSQL integration coverage to verify the tracking schema and persisted lifecycle transitions.

Verification results:

- Focused migration runner tests: successful.
- `./gradlew postgresIntegrationTest --tests ...PostgresPersistenceIntegrationTest`: successful.
- `git diff --check`: successful.

### Migration execution safety and complete source pagination

- Made migration execution opt-in through the documented `migration.run=true` property; selecting the migration profile
  alone no longer copies data or exits the application.
- Added configurable DynamoDB page sizes and cursor traversal for every migrated entity type, removing the previous
  first-100-record limit.
- Added a no-write dry-run mode, retained insert-if-absent rerun behavior, and corrected the copy order to departments,
  students/profiles, instructors, courses, and enrollments.
- Updated the README and implementation plan to reflect the implemented PostgreSQL controllers, migration runner, and
  the remaining Phase 5 tracking/retry/reconciliation work.

Verification results:

- `./gradlew test --tests com.abc.studentportal.common.migration.DynamoToPostgresMigrationRunnerTest`: successful.
- Regression coverage verifies multi-page traversal and that dry-run performs no target writes.
- `./gradlew compileJava test`: successful.

### PostgreSQL JPA adapters and relationship mappings

- Added profile-scoped PostgreSQL adapters for all six application repository ports, mapping domain records to separate
  JPA entities and Spring Data repositories.
- Corrected relational mappings for Department references, Student/Profile shared primary keys, Instructor/Course
  references, and Enrollment foreign keys with active-enrollment queries.
- Added LocalStack RDS port exposure and verified compilation, unit tests, and PostgreSQL Testcontainers integration.

### LocalStack RDS PostgreSQL foundation revision

- Documented LocalStack Pro RDS as the supported local PostgreSQL engine, with
  Terraform-provisioned instance and Secrets Manager credentials.
- Removed the standalone-Compose assumption from the PostgreSQL schema and
  implementation plan; Testcontainers remains the isolated test database.
- Added a LocalStack RDS PostgreSQL runbook, startup timing caveats, and
  Flyway/profile guidance to `README.md` and `docs/postgresql-schema.md`.

### DynamoDB datasource complete

- Closed every blocker from the final DynamoDB audit: OpenAPI/Swagger, DynamoDB health, correlation-aware safe request
  logging, safe startup configuration reporting, controller-to-LocalStack integration coverage, and consolidated
  architecture/limitations documentation.
- Generated a 22-resource-path OpenAPI contract with RFC 9457 errors, validation metadata, pagination parameters, and
  explicit opaque DynamoDB cursor semantics; Swagger UI is available at `/swagger-ui.html`.
- Added a six-table health contributor, inbound/generated `X-Correlation-ID` propagation with MDC, structured request
  completion logs that exclude bodies and query values, and startup output that excludes credentials.
- Added full-context REST integration coverage backed by Testcontainers LocalStack, while retaining focused domain,
  MVC, persistence, concurrency, transaction, cursor, and seed tests.
- Completed `docs/architecture-dynamodb.md` and `docs/dynamodb-limitations.md` with item/access-path/enrollment diagrams,
  transaction and consistency limits, operational risks, repair concerns, and PostgreSQL migration implications.

Verification results:

- `./gradlew clean check`: successful; 39 unit/MVC tests and 12 LocalStack integration tests passed.
- Terraform format/validate and the real-state plan passed; a disposable six-table create, immediate no-change plan,
  and destroy cycle passed, leaving only the six intended tables.
- The seeded 14-request live HTTP smoke workflow passed; `/v3/api-docs` returned 200, `/swagger-ui.html` redirected to
  the UI, actuator health returned `UP`, and both health/OpenAPI responses carried correlation IDs.
- Compose configuration, shell syntax, Postman JSON, whitespace, and secret audits passed.
- Final decision recorded in `docs/dynamodb-phase-audit.md`: no DynamoDB-phase blocker remains.

### Exact alternate-key reads and strict query contracts

- Added zero-or-one repository reads for Department code, Student number/email, Instructor number/email, and Course
  code using their existing GSIs with a one-item query bound.
- Exposed exact alternate keys as mutually exclusive collection filters with normalized inputs and the existing response
  envelope (`limit: 1`, no next cursor).
- Added a profile-scoped MVC interceptor that derives allowed query parameters from each handler and rejects unknown
  parameters before controller execution.
- Explicitly reject exact-key/cursor combinations and exact-key combinations with relationship or status filters.
- Updated API, access-pattern, audit, and progress documentation.

Verification results:

- `./gradlew clean check`: successful; 37 unit/MVC tests and 11 LocalStack integration tests passed.
- MVC coverage verifies successful/empty exact reads plus unknown, conflicting, and cursor-incompatible parameters.
- LocalStack coverage verifies entity-returning reads through all seven alternate-key GSIs.
- The expanded 14-request live HTTP smoke workflow passed, including exact email lookup and unknown-filter rejection.
- `git diff --check`: successful.

### Terraform convergence recovery

- Reproduced the six-table configuration with an isolated state and unique table prefix; the immediate refreshed second
  plan exited `0`, proving the module and locked AWS provider converge from clean table creation.
- Isolated the real drift to two Enrollment GSIs previously added with `UpdateTable`. LocalStack returned explicit
  unlimited on-demand throughput for those indexes, destabilizing the provider's inline-GSI set comparison.
- Evaluated the stable standalone GSI resource, but rejected it for this local workflow because LocalStack 4.14 does not
  return the warm-throughput status required by the AWS provider waiter.
- Backed up and recreated only the local Enrollment table so all six GSIs were created atomically, reconciled the
  deterministic seed counters, and restored the seed through the normal transactional seeder.
- Removed both disposable table sets after testing.

Verification results:

- `terraform -chdir=infrastructure/local validate`: successful.
- `terraform -chdir=infrastructure/local plan -detailed-exitcode -no-color`: exit `0`; no changes.
- Enrollment table: `ACTIVE`; all six configured GSIs: `ACTIVE`.
- Enrollment seed state: 15 physical items representing six Enrollments plus locks and durable relationship edges.

### Final DynamoDB phase audit — tag withheld

- Traced the master prompt's DynamoDB, Terraform, API, testing, documentation, observability, and operational requirements
  to the current implementation.
- Re-ran the full Gradle/LocalStack suite, Terraform formatting/validation/plan, Compose/artifact/secret/scan checks, and
  the live 12-request seeded HTTP workflow.
- Confirmed the core implementation is functional, but withheld `dynamodb-complete` because OpenAPI, DynamoDB-specific
  observability, exact alternate-key reads, strict unsupported-query handling, automated REST-to-LocalStack coverage,
  required documentation/diagrams, and Terraform convergence remain incomplete.
- Recorded the evidence and ordered remediation plan in `docs/dynamodb-phase-audit.md`.

Verification results:

- `./gradlew clean check`: successful; 35 unit/MVC tests and 11 LocalStack integration tests passed.
- `terraform fmt -check -recursive infrastructure`: successful.
- `terraform -chdir=infrastructure/local validate`: successful.
- `terraform ... plan -detailed-exitcode`: exit `2` at audit time; resolved by the later Terraform convergence recovery.
- `docker compose config --quiet`, shell syntax, Postman JSON, scan audit, secret audit, and whitespace checks: successful.
- `./scripts/dynamodb-api-smoke.sh`: successful against the already-running application on port 8080.

### Documentation reconciliation after the complete DynamoDB API surface

- Reconciled the table/index inventory with the two durable relationship-edge GSIs and typed Enrollment records.
- Updated architecture, implementation, domain deletion, REST filtering, ADR, seed workflow, README, Postman, and
  developer-help documentation to describe the implemented application rather than earlier deferred checkpoints.
- Distinguished historical progress entries from current limitations and consolidated the LocalStack 4.14/AWS provider
  GSI waiter/computed-throughput behavior into the development operations guide.
- Cross-checked all 38 controller mappings, current DynamoDB filters, 35 unit/MVC tests, and 11 LocalStack integration
  tests against the documents.

Verification results:

- `jq empty postman.json`: successful.
- Markdown stale-state audit: no current section claims controllers or derived routes are pending.
- `git diff --check`: no whitespace errors.

### Deduplicated Student/Course relationship views

- Added a deterministic durable relationship edge per Student/Course pair to the Enrollments table. Enrollment creation
  maintains the edge in the same transaction, and re-enrollment overwrites the same edge instead of creating duplicates.
- Added two sparse edge GSIs and opaque cursor queries for Student-to-Courses and Course-to-Students.
- Added bounded, strongly consistent `BatchGetItem` hydration that preserves edge order and retries unprocessed keys.
- Exposed `GET /students/{id}/courses` and `GET /courses/{id}/students` with parent existence validation and normal cursor
  response DTOs.
- Made the development seeder repair deterministic edges for seed Enrollments created before this checkpoint.
- Added MVC coverage and LocalStack coverage for re-enrollment deduplication, multi-page traversal, reverse lookup, and
  query-bound cursor rejection.

Verification results:

- `./gradlew clean check`: successful; 35 unit/MVC tests and 11 LocalStack integration tests passed.
- `terraform fmt -check -recursive infrastructure`: successful.
- `terraform -chdir=infrastructure/local validate`: successful; all six configured Enrollment GSIs report `ACTIVE` in
  LocalStack. LocalStack 4.14 timed out the AWS provider waiter after creating the first new GSI, so the second configured
  GSI was applied through the emulator's DynamoDB API and then verified directly. The AWS provider continues to show a
  computed-throughput set diff against LocalStack despite identical index names and keys; this emulator-only drift must
  be re-audited before the final DynamoDB tag.
- Expanded 12-request live HTTP smoke workflow: successful, including both derived routes.
- `git diff --check`: no whitespace errors.

### DynamoDB development seeding and live HTTP workflow

- Added an opt-in `local-dynamodb` seeder with deterministic UUIDs and idempotent strong-read guards.
- Seed writes use normal repositories and transactions, preserving uniqueness claims, dependency/enrollment counters,
  capacity, versions, and active locks instead of bypassing application persistence rules.
- Added three Departments, ten Students and Profiles, five Instructors, ten Courses, and six Enrollments with varied
  statuses and an authoritative full-course example.
- Added Makefile run/seed/smoke commands, safe environment defaults, an executable HTTP smoke script, and a dedicated
  developer workflow document.
- Added LocalStack coverage that runs the seeder twice and verifies completeness, stable state, varied statuses, and full
  capacity.
- Applied Terraform to an existing healthy LocalStack Pro instance, launched the seeded application, and passed the live
  ten-request HTTP workflow across health, resources, relationships, cursors, validation, unsupported filters, and 404s.

Verification results:

- `./gradlew clean check`: successful; 34 unit/MVC tests and 10 LocalStack integration tests passed.
- `bash -n scripts/dynamodb-api-smoke.sh`: successful.
- `make -n app-run-dynamodb-seeded api-smoke`: successful.
- `terraform -chdir=infrastructure/local apply -auto-approve`: successful; six tables matched configuration.
- `./scripts/dynamodb-api-smoke.sh`: successful against the seeded running application.
- `git diff --check`: no whitespace errors.

### DynamoDB REST controllers

> Historical checkpoint: the two derived relationship routes noted below were implemented by the later deduplicated
> Student/Course relationship-view checkpoint.

- Added profile-scoped controllers for Departments, Students/Profiles, Instructors, Courses, and Enrollments.
- Exposed create/get/update/status/delete workflows with validated DTOs, `Location` headers, optimistic versions, and
  the documented Enrollment DELETE-as-drop behavior.
- Exposed opaque cursor catalogs and direct relationship-index collections, with explicit rejection of unsupported
  DynamoDB filter combinations instead of scans or unbounded in-memory filtering.
- Added request-parameter Problem Details handling for missing versions, malformed UUIDs, and invalid enum/query values.
- Added MVC coverage for creation, response mapping, cursor envelopes, validation, filter rejection, and versioned delete.
- Kept Student-to-Courses and Course-to-Students unexposed pending a bounded, deduplicated batch-composition capability.

Verification results:

- `./gradlew clean check`: successful; 34 unit/MVC tests and 9 LocalStack integration tests passed.
- `git diff --check`: no whitespace errors.

### Authoritative parent dependency counters

- Added Department Student/Instructor/Course counters and an Instructor Course counter, initialized on entity creation
  and preserved across ordinary updates.
- Made Student, Instructor, and Course create, relationship-move, and delete operations update affected parent counters
  and versions in the same DynamoDB transaction as the child write.
- Made Department and Instructor deletion conditionally require zero authoritative counters. Parent deletion and a
  concurrent relationship write now contend on the same item, so both cannot commit even while GSIs are stale.
- Prevented moving an Instructor between Departments while Courses remain assigned, preserving the Course
  department/instructor invariant.
- Added LocalStack coverage for counter transfer and release plus concurrent Department-delete/Student-create and
  Instructor-delete/Course-create races.

Verification results:

- `./gradlew clean check`: successful; 29 unit/MVC tests and 9 LocalStack integration tests passed.
- `terraform fmt -check -recursive infrastructure`: successful.
- `terraform -chdir=infrastructure/local validate`: successful.
- `git diff --check`: no whitespace errors.

### Enrollment transactions and dependency-aware deletion

- Added an Enrollment application service for enroll, status-transition, and drop use cases; physical Enrollment deletion
  was removed from the repository contract so history cannot bypass capacity/lock repair.
- Implemented a cross-table create transaction that validates active Student/open Course state, atomically enforces
  capacity, writes Enrollment, and creates the deterministic active-pair lock.
- Implemented transactional status changes with optimistic Enrollment versions, capacity deltas, final-grade/domain
  transitions, and owned active-lock deletion.
- Added authoritative Student/Course enrollment-history counters. Enrollment creation updates their counters and versions,
  allowing deletion to reject history and conflict safely with concurrent enrollment even during GSI propagation delay.
- Added dependency-check capabilities and delete methods for Departments, Students, Instructors, and Courses. Student
  deletion atomically removes its optional Profile and uniqueness claims.
- Added LocalStack coverage for duplicate active enrollment, full capacity, rollback, drop/re-enroll, completion,
  stale transitions, concurrent last-seat attempts, history deletion guards, and atomic Student/Profile deletion.
- Updated domain, relationship, table-design, transaction, implementation, README, and progress documentation. This
  historical checkpoint's eventual-GSI deletion limitation was closed by the later parent-counter checkpoint.

Verification results:

- `./gradlew clean check`: successful; 29 unit/MVC tests and 7 LocalStack integration tests passed.
- `terraform fmt -check -recursive infrastructure`: successful.
- `terraform -chdir=infrastructure/local validate`: successful.
- `git diff --check`: no whitespace errors.

### Relationship-validating services and transactional uniqueness

- Added profile-scoped application services for Department, Student/Profile, Instructor, and Course create/update/status
  use cases with an injectable UTC Clock.
- Added strong reference validation for Student/Instructor departments, StudentProfile ownership, and Course department
  and instructor references; Courses also enforce that their instructor belongs to the same department.
- Added deterministic sparse claim records for department codes, student numbers/emails, employee numbers/emails, and
  course codes. Normal GSIs remain read paths and never act as uniqueness authorities.
- Made entity and claim creation atomic, made key-changing updates atomically release/acquire claims under optimistic
  version conditions, and made physical deletes atomically release owned claims.
- Translated transaction cancellation to the existing conflict contract; failed multi-claim and conflicting-update
  transactions leave both authoritative records and prior claims unchanged.
- Added service unit coverage plus LocalStack rollback, key reuse, deletion release, and simultaneous-writer race coverage.
- Added dedicated DynamoDB relationship and transaction documentation, including the then-remaining cross-table deletion
  race that the later authoritative parent-counter checkpoint closed before destructive endpoints were exposed.

Verification results:

- `./gradlew clean check`: successful; 28 unit/MVC tests and 5 LocalStack integration tests passed.
- `terraform fmt -check -recursive infrastructure`: successful.
- `terraform -chdir=infrastructure/local validate`: successful.
- `git diff --check`: no whitespace errors.

### DynamoDB cursor pagination and query capabilities

- Added a domain-level immutable `CursorPage` and database-specific capability ports without adding DynamoDB pagination
  semantics to the common CRUD repositories.
- Implemented bounded Enhanced Client queries for every documented catalog and relationship GSI across Departments,
  Students, Instructors, Courses, and Enrollments.
- Added normalized student last-name prefix queries and inclusive enrollment date-range queries using sort-key conditions.
- Added a versioned URL-safe opaque cursor codec for typed `LastEvaluatedKey` attributes. Cursors are bound to physical
  table, index, partition, prefix, and range identity; malformed or mismatched cursors return the invalid-request contract.
- Fixed derived timestamp sort keys to use fixed-width signed-epoch seconds plus nanoseconds and UUID. Stored timestamps
  remain ISO-8601, while GSI ordering is chronological across whole, fractional, and pre-epoch instants.
- Added cursor codec unit tests and LocalStack tests for multi-page traversal, no duplicates, query mismatch rejection,
  every capability query, last-name prefix selection, and inclusive enrollment date bounds.

Verification results:

- `./gradlew clean check`: successful; 26 unit/MVC tests and 4 LocalStack integration tests passed.
- `terraform fmt -check -recursive infrastructure`: successful.
- `terraform -chdir=infrastructure/local validate`: successful.
- `git diff --check`: no whitespace errors.

### DynamoDB persistence-foundation review

- Audited the six Enhanced Client schemas, mappings, adapters, Spring profile wiring, Terraform table/index definitions,
  access-pattern documentation, and checkpoint claims against the master prompt.
- Made the deterministic active-enrollment lookup explicitly strongly consistent, matching the documented concurrency protocol.
- Made missing Course updates return the same conflict contract as other missing/stale optimistic updates instead of a null-pointer failure.
- Corrected uniqueness documentation: GSIs are eventually consistent and cannot enforce uniqueness; the next application-service
  checkpoint will transactionally maintain deterministic alternate-key claims.
- Corrected stale README text that still described LocalStack, Terraform, and DynamoDB profiles as future work.
- Added mapper round-trip tests for every domain/record boundary, including normalization, nullable fields, timestamps,
  versions, record discriminators, and all derived GSI sort keys.
- Expanded LocalStack tests to create and read all six domain records, exercise every GSI, verify every exact lookup adapter,
  verify profile partition-key behavior, reject missing/stale mutations, preserve occupied-seat state during Course updates,
  and prove active-lock records are strongly visible but absent from logical enrollment indexes.
- Extended the Spring profile test to verify all six repository ports resolve to DynamoDB adapters.

Verification results:

- `./gradlew clean check`: successful; 23 unit/MVC tests and 3 LocalStack integration tests passed.
- `terraform fmt -check -recursive infrastructure`: successful.
- `terraform -chdir=infrastructure/local validate`: successful.
- `git diff --check`: no whitespace errors.

### DynamoDB architecture revision — six source tables

- Reviewed the supplied reference project inventory of 13 DynamoDB tables, heterogeneous keys/GSIs, embedded arrays, JSON strings, cross-table logical joins, legacy attributes, and migration exclusions.
- Superseded the initial single-table ADR while preserving it as project decision history.
- Added ADR 0009 explaining why six source tables better serve the project’s migration-learning objective.
- Rewrote the access-pattern and table-design documents around Departments, Students, StudentProfiles, Instructors, Courses, and Enrollments tables.
- Replaced enrollment relationship copies with authoritative enrollment records indexed by student/course GSIs.
- Defined typed active-enrollment lock records in the Enrollment table and authoritative occupied-seat state in Course records for future cross-table transactions.
- Refactored the Terraform module to accept arbitrary partition keys and GSI key schemas, then instantiated it six times from a table definition map.
- Updated application configuration and `.env.example` with six independently configurable table names.
- Updated README and architecture documentation for multi-table operation and migration concerns.

Verification results:

- `terraform fmt -check -recursive infrastructure`: successful.
- `terraform validate`: successful with no warnings.
- Reviewed Terraform plan: 6 resources added, 1 obsolete empty table destroyed, no unrelated changes.
- Terraform apply: successful, exactly 6 added and 1 destroyed.
- Live LocalStack verification: all six tables `ACTIVE`, expected partition keys, and all documented GSIs present.
- Post-apply Terraform plan: no changes.
- `./gradlew clean check`: successful, 17 tests passed.

Migration-learning additions:

- Per-table source pagination and checkpoints.
- Referentially ordered reads across six independent source tables.
- Cross-table orphan and count reconciliation.
- Planned fixtures for legacy aliases, invalid enums, duplicates, missing references, and JSON-encoded source values.

### DynamoDB infrastructure and access-pattern design

> Historical checkpoint: this initial single-table decision was later superseded after reviewing the multi-table
> reference migration. See the architecture revision recorded below and ADR 0009.

- Inventoried entity, relationship, enrollment, integrity, deletion, filtering, ordering, consistency, and transaction access patterns before selecting keys.
- Selected and documented a single-table design with authoritative items, unique claims, enrollment copies, an active-pair claim, and course capacity state.
- Defined four sparse GSIs for entity catalogs, department membership, status, and instructor courses.
- Explicitly documented unsupported DynamoDB filters/sorts and the intentional migration-only scan exception.
- Added LocalStack Pro Compose configuration with a required external token, health check, persistence, named volume, and dedicated network.
- Added profile-scoped, validated `DynamoDbProperties` and `local-dynamodb` configuration.
- Added a reusable Terraform DynamoDB module and local root configuration with Terraform 1.14 and AWS provider 6 constraints.
- Generated and committed the provider lock selection for HashiCorp AWS provider 6.54.0.
- Replaced deprecated GSI hash/range arguments with current `key_schema` blocks.
- Applied Terraform to LocalStack Pro and verified the table plus all four GSIs are active.

Verification results:

- `docker compose config --quiet`: successful.
- `terraform init -backend=false`: successful; AWS provider 6.54.0 installed.
- `terraform validate`: successful with no warnings after the GSI syntax correction.
- Initial no-refresh plan: one table to add, no other actions.
- `terraform apply -auto-approve`: successful; one resource added.
- Post-apply plan: no changes.
- Live `describe-table`: table `ACTIVE`; composite `PK`/`SK` and four expected GSIs all `ACTIVE`.
- `./gradlew clean check`: successful, 17 tests passed.

Operational findings:

- The first project container startup found host ports `4510` and then `4566` already allocated by an existing healthy LocalStack Pro container.
- The optional external-service port range was removed because DynamoDB needs only port `4566`.
- The existing user-managed LocalStack container was preserved and used for verification; the failed project-owned container was removed.

### Phase 1 — core domain and REST contracts

> Historical checkpoint: database adapters, application services, and concrete controllers described as future work in
> this section were implemented by the later DynamoDB checkpoints above.

- Added persistence-neutral records for Department, Student, StudentProfile, Instructor, Course, and Enrollment.
- Added student, course, and enrollment enums plus explicit course/enrollment transition rules.
- Enforced local field, audit, grade, drop-timestamp, capacity, and credit invariants in the domain.
- Added immutable validated request/response DTOs and explicit domain-to-response mappers.
- Added bounded DynamoDB cursor and PostgreSQL page request/response contracts without pretending their semantics match.
- Added filter contracts and explicit repository ports for future database adapters.
- Added RFC 9457 Problem Details handling for validation, malformed input, missing resources, conflicts, domain rules, invalid arguments, and unexpected failures.
- Added unit, validation, mapping, pagination, and MVC error-contract tests.

Verification results:

- First MVC run exposed missing explicit constructor injection in the test slice; corrected.
- Second MVC run exposed that a nested fixture controller was not selected by Spring Boot 4.1; moved it to a top-level test fixture.
- First clean check exposed a fluent assertion typo at test compilation; corrected.
- Final `./gradlew clean check`: successful, 16 tests passed, 5 actionable tasks executed.
- `git diff --check`: no whitespace errors.

Architecture decisions:

- Domain records validate only rules that do not require database state.
- Uniqueness, relationship existence, deletion dependency checks, same-department instructor assignment, and capacity concurrency remain application-service responsibilities.
- Repository ports contain common operations only; database-specific list/query capabilities will be added after DynamoDB access-pattern design.
- Concrete controllers are deferred until real application services can back them; MVC slice tests verify transport contracts without placeholder runtime beans.
- Enrollment history is retained, while course and enrollment terminal states reject invalid transitions.

### Phase 0 — Java 25 / Spring Boot 4 foundation

- Reviewed the repository and master prompt; confirmed the starter repository was already initialized.
- Verified Java 25.0.1, Gradle 9.5.1, Git 2.53.0, Docker 29.6.1, Docker Compose 5.3.0, and Terraform 1.14.9.
- Documented the persistence-neutral domain model, lifecycle rules, deletion policies, REST contracts, layered architecture, and implementation roadmap.
- Added a Gradle version catalog and the foundation-only Actuator and Jakarta Validation starters.
- Exposed only the Actuator health endpoint and kept health details private.
- Added context and health assertions.
- Corrected root ignore rules so the required `.env.example` and `.terraform.lock.hcl` can be committed in later phases.

Commands executed:

```text
java -version
git --version
docker --version
docker compose version
terraform version
./gradlew --version
./gradlew test
./gradlew clean check
./gradlew bootRun
curl -sS -i http://127.0.0.1:8080/actuator/health
git diff --check
```

Verification results:

- The original starter test passed.
- The first modified build exposed a Spring Boot 4.1 package change for health types; imports were corrected from the pre-4.1 package to `org.springframework.boot.health` packages.
- Final `./gradlew clean check`: successful, 5 actionable tasks executed.
- HTTP health verification: `200 OK` with `status: UP`.
- `git diff --check`: no whitespace errors.

Architecture decisions:

- Keep API DTOs, domain objects, DynamoDB records, and JPA entities separate.
- Use application services for cross-aggregate rules and repository-backed uniqueness checks.
- Treat Enrollment as an explicit association entity and preserve enrollment history.
- Expose DynamoDB cursor pagination and PostgreSQL page pagination as honest capability differences.
- Use common repository ports only for common behavior and database-specific capability ports for queries and migration.

Risks and follow-ups:

- DynamoDB keys and GSIs must be selected only after the access-pattern document is complete.
- Enrollment capacity requires separate, concurrency-tested transaction strategies for DynamoDB and PostgreSQL.
- Spring Boot 4.1 modularized health APIs; future examples must be checked against the selected version.
- LocalStack Pro requires a developer-supplied token that must never be committed.

## In Progress

- PostgreSQL persistence is connected through profile-scoped JPA adapters. The remaining work is PostgreSQL-specific
  transaction semantics, CRUD/API parity, and migration tooling; DynamoDB remains the comparison/source adapter.

## Blocked

## Next Tasks

- Add PostgreSQL transaction services (capacity, active-enrollment locking, dependency-safe deletes) and validate
  application CRUD/API parity against LocalStack RDS.
- Add explicit DynamoDB-to-PostgreSQL migration/reconciliation tooling after transaction behavior is covered.

### PostgreSQL persistence integration scaffold

- Added a tagged `PostgresPersistenceIntegrationTest` using a PostgreSQL 17 Testcontainers database.
- Added the `postgresIntegrationTest` Gradle task; `check` now runs both DynamoDB and PostgreSQL integration suites.
- The test confirms Flyway creates the initial relational schema before JPA repositories are introduced.

### PostgreSQL infrastructure foundation

- Added Flyway `V1__initial_schema.sql` for the six relational tables, foreign keys, status and counter checks,
  alternate-key uniqueness, relationship indexes, and the active-enrollment partial unique index.
- Documented PostgreSQL schema responsibilities and deferred transaction locking in `docs/postgresql-schema.md`.

### PostgreSQL JPA entities, adapters, and LocalStack RDS validation

- Added six separate JPA entities and Spring Data repositories with shared `BaseEntity` identity/timestamp mapping,
  correct foreign keys, `@MapsId` student-profile identity, optimistic locking, and profile-scoped adapters.
- LocalStack Pro RDS PostgreSQL is the supported development datasource; Flyway applies `V1__initial_schema.sql`.
- Added real LocalStack RDS CRUD integration coverage and a dedicated `localstackRdsIntegrationTest` task. Hibernate
  owns entity timestamps through `@CreationTimestamp` and `@UpdateTimestamp`; Lombok supplies accessors while entity
  mutation remains controlled.

### Six-table DynamoDB persistence foundation

- Added AWS SDK 2 DynamoDB/Enhanced Client and Testcontainers LocalStack dependencies through the version catalog.
- Added separate annotated records and explicit domain mappings for all six source tables; API/domain/JPA concerns remain separate.
- Added profile-scoped low-level and Enhanced clients plus typed bindings for every configured table.
- Added conditional create, strongly consistent primary-key reads, version-conditioned delete, and Enhanced Client optimistic updates.
- Added concrete adapters for all repository ports and exact alternate-key existence checks through the documented GSIs.
- Preserved the internal course occupied-seat counter during ordinary domain updates so later enrollment transactions own it.
- Added `dynamodbIntegrationTest`; its isolated LocalStack container provisions all six tables and every expected GSI.
- Verified schema parity, CRUD, GSI lookup, duplicate primary-key rejection, version increments, stale-write rejection, and delete.

Verification results:

- `./gradlew testClasses`: successful.
- `./gradlew dynamodbIntegrationTest`: successful, 2 integration tests passed.
- `./gradlew clean check`: successful; unit/MVC and LocalStack integration suites all passed.
- `git diff --check`: no whitespace errors.
- LocalStack integration tests require Docker only and do not depend on the project Compose stack or Pro token.

### PostgreSQL transactional application services

- Added PostgreSQL-profiled transactional services for departments, instructors, courses, students, profiles, and enrollments.
- Added a PostgreSQL dependency checker backed by authoritative JPA relationship queries.
- Added LocalStack RDS service-level tests covering invalid relationship rollback and stale optimistic-lock updates.
- Added explicit course version validation before persistence to reject stale updates.
- Regression verification: `./gradlew test dynamodbIntegrationTest localstackRdsIntegrationTest` successful.

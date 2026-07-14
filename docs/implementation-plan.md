# Implementation plan

Each phase ends with compilation, tests, documentation, a focused commit, and a report in `progress-report.md`. A failing verification blocks the checkpoint.

## Phase 0 — foundation

Document the domain, REST contracts, architecture, and roadmap. Configure Java 25, Gradle 9, Spring Boot 4, Actuator health, and a context/health test. Do not add persistence.

## Phase 1 — core contracts

Implement persistence-neutral domain behavior, API DTOs, validation, errors, application ports, and controller contracts. Use focused unit and MVC tests without selecting a database.

Status: complete for domain behavior, DTO and error contracts, mapping, filters, and persistence ports. Concrete
controllers were intentionally deferred at this checkpoint until repository-backed services existed; Phase 3 has since
implemented them. MVC slice tests prove validation and the Problem Details contract independently.

## Phase 2 — DynamoDB infrastructure and access patterns

Document every access pattern first, select and justify the table design, then provision it with Terraform on LocalStack Pro.

## Phase 3 — DynamoDB application

Implement Enhanced Client records and adapters, indexed queries, cursor pagination, conditional writes, transactional enrollment, integration tests, seed support, and DynamoDB documentation. Commit and tag `dynamodb-complete` only after verification.

Status: persistence foundation complete. Six distinct record schemas and domain mappings, profile-scoped client/table
configuration, primary-key CRUD adapters, optimistic locking, exact alternate-key GSI checks, and LocalStack
Testcontainers coverage are implemented. Database-specific capability ports now provide bounded, opaque cursor queries
for every documented catalog and relationship GSI, including student last-name prefixes and enrollment date ranges.
Application services strongly validate entity relationships, and transactional sparse claims enforce alternate-key
uniqueness across create, update, and delete. Cross-table enrollment transactions now enforce eligibility, capacity,
active-pair uniqueness, transitions, and Student/Course history counters. Dependency-aware services guard deletion and
atomically remove Student/Profile. Transactional parent dependency counters serialize Department/Instructor deletion
against concurrent child creation, movement, and deletion. Concrete DynamoDB controllers now expose CRUD, status,
profile, enrollment, cursor catalog, and direct relationship-index operations. Explicit idempotent development seeding
and a live HTTP smoke workflow are implemented and verified. Bounded, deduplicated Student/Course derived views now use
transactional relationship edges, sparse GSIs, and strongly consistent batch hydration.

## Phase 4 — PostgreSQL infrastructure and model

Add PostgreSQL Compose support, Flyway migrations, separate JPA entities, repositories, filtering, page pagination, and relational transaction enforcement.

## Phase 5 — persistence switch and migration

Make PostgreSQL the normal primary adapter. Retain DynamoDB as a migration source and build explicit, restartable, batched, idempotent migration with tracking, retry, dry-run, and rejected-record handling.

## Phase 6 — reconciliation and parity

Generate reconciliation reports, test API parity and documented differences, add optional sampled dual-read verification, complete operational documentation, run the full suite, and tag `postgres-migration-complete`.

## Verification strategy

Unit tests cover local domain rules and mapping. MVC tests cover contracts and Problem Details. Dedicated Testcontainers suites cover DynamoDB, PostgreSQL, concurrency, and end-to-end migration. `./gradlew clean check` is the final required verification; separated integration tasks will be documented if resource cost makes them opt-in during development.

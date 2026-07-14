# Progress Report

## Completed Tasks

### Phase 1 — core domain and REST contracts

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

- None.

## Blocked

## Next Tasks

- Create `docs/dynamodb-access-patterns.md` before choosing any DynamoDB keys.
- Decide and document the DynamoDB table, item ownership, denormalization, and GSI design.
- Add LocalStack Pro Compose configuration and typed `local-dynamodb` properties.
- Provision the approved table design with the required Terraform module and local stack.
- Validate Terraform and create the `infra: provision LocalStack DynamoDB with Terraform` checkpoint.

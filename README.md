# Student Portal API

A learning-oriented backend that first implements a university course registration portal with DynamoDB and then migrates the same application and data to PostgreSQL. The project makes database-specific tradeoffs visible instead of hiding them behind a misleadingly uniform persistence layer.

## Current checkpoint

The DynamoDB foundation now provisions six domain-oriented source tables and maps persistence-neutral domain objects to
separate Enhanced Client records. Profile-scoped clients and table bindings, conditional CRUD adapters, optimistic
locking, exact GSI lookups, and isolated LocalStack integration tests are in place. Cursor pagination over every
documented catalog and relationship GSI is also implemented through database-specific capability ports. Transactional
alternate-key claims and relationship-validating application services are now implemented for Departments, Students,
Profiles, Instructors, and Courses. Cross-table enrollment/capacity transactions, active locks, history counters, and
dependency-aware deletion are implemented. Authoritative parent counters now close Department/Instructor deletion races
with concurrent relationship writes. DynamoDB-profile controllers now expose CRUD, status transitions, profiles,
enrollment workflows, cursor catalogs, and direct relationship collections. An explicitly enabled idempotent DynamoDB
seeder and live HTTP smoke workflow are available. Deduplicated Student/Course many-to-many views are implemented with
transactional relationship edges and bounded batch hydration.
Exact alternate-key collection reads, strict query-parameter rejection, Swagger/OpenAPI, correlation IDs, contextual
request logs, DynamoDB health, safe startup summaries, and full MVC-to-LocalStack integration coverage are also complete.

## Repository layout

```text
.
├── student-portal-api/   Spring Boot application and Gradle wrapper
├── docs/                 Architecture, domain, and API decisions
├── infrastructure/       Terraform (introduced in the DynamoDB phase)
├── scripts/              Verified local API smoke workflows
├── postman.json          Importable Postman collection
├── compose.yml           Local development services (introduced by phase)
└── Makefile              Verified developer shortcuts (introduced by phase)
```

## Prerequisites

- Java 25
- Docker with Docker Compose
- Terraform 1.14.x
- Git

The Gradle 9 wrapper is included; a system Gradle installation is not required.

## Build and test

```shell
cd student-portal-api
./gradlew clean check
```

`check` includes the isolated LocalStack/Testcontainers persistence suite. It requires a running Docker engine but does
not require the project Compose stack or a LocalStack token. Run only that suite with `./gradlew dynamodbIntegrationTest`.

## Run the health-only application

```shell
cd student-portal-api
./gradlew bootRun
```

The initial health endpoint is available at `http://localhost:8080/actuator/health`.

<details>
<summary><b>dynamo-to-pg</b></summary>

## LocalStack Pro and DynamoDB infrastructure

Export your LocalStack token; do not place it in a committed file:

```shell
export LOCALSTACK_AUTH_TOKEN="<your-token>"
make compose-up
```

Provision the six access-pattern-driven source tables:

```shell
make tf-init
make tf-validate
make tf-plan
make tf-apply
```

Run the application with the typed local configuration profile:

```shell
make app-run-dynamodb
```

The profile reads `AWS_REGION`, `DYNAMODB_ENDPOINT`, and six table-name variables; safe local defaults are shown in
`.env.example`. The application does not create infrastructure on startup. The tables mirror domain boundaries for
migration learning, but reference attributes are not foreign keys and DynamoDB performs no joins.

To load deterministic development data and test the running API:

```shell
make seed-dynamo-data
# In another terminal:
make api-smoke
```

The seeder runs only with the `local-dynamodb` profile and `STUDENT_PORTAL_SEED_ENABLED=true`. It is disabled by default,
safe to rerun, and creates three Departments, ten Students with Profiles, five Instructors, ten Courses, and six
Enrollments. The data includes multiple statuses and a full two-seat Course. See
[DynamoDB development data](docs/dynamodb-development-data.md) for its behavior and stable smoke-test identifiers.

For interactive testing, import [postman.json](postman.json) into Postman. The collection includes seeded reads, error
examples, and an ordered Department → Instructor → Student/Profile → Course → Enrollment workflow that captures generated
IDs and optimistic versions automatically. Keep the seeded server running while using the collection.

Swagger UI is available at `http://localhost:8080/swagger-ui.html`; the generated OpenAPI JSON is available at
`http://localhost:8080/v3/api-docs`. The contract documents validation constraints, RFC 9457 errors, exact alternate-key
filters, and DynamoDB opaque cursor behavior.

To remove the local tables and services:

```shell
make tf-destroy
make compose-down
```

If port `4566` is already in use, check for an existing LocalStack container before starting another. A healthy existing
LocalStack instance can be used by Terraform at the same endpoint; do not stop unrelated containers.

## PostgreSQL datasource and migration workflow

Start LocalStack and provision its RDS-compatible PostgreSQL instance:

```shell
make postgres-up
make postgres-health
```

When the application starts with the PostgreSQL profile, Flyway automatically applies the relational schema from
`V1__initial_schema.sql`:

```shell
make app-run-postgres
```

The local JDBC endpoint is `jdbc:postgresql://localhost.localstack.cloud:4510/student_portal`. PostgreSQL entities,
repositories, transactional services, and LocalStack RDS integration tests are implemented.

The DynamoDB-to-PostgreSQL data-copy runner is not implemented yet. Starting the PostgreSQL profile creates and migrates
the schema, but it does not copy existing DynamoDB records. Until that migration runner is added, data must be created
through PostgreSQL services/tests independently. PostgreSQL REST controllers are also still pending; the current HTTP
controllers are DynamoDB-profiled.

</details>

## Design documents

- [Architecture overview](docs/architecture-overview.md)
- [Domain model](docs/domain-model.md)
- [REST API contracts](docs/rest-api-contracts.md)
- [Implementation plan](docs/implementation-plan.md)
- [DynamoDB access patterns](docs/dynamodb-access-patterns.md)
- [DynamoDB application architecture](docs/architecture-dynamodb.md)
- [DynamoDB table design](docs/dynamodb-table-design.md)
- [DynamoDB relationships](docs/dynamodb-relationships.md)
- [DynamoDB transactions](docs/dynamodb-transactions.md)
- [DynamoDB development data](docs/dynamodb-development-data.md)
- [DynamoDB limitations](docs/dynamodb-limitations.md)

## LocalStack RDS PostgreSQL

PostgreSQL development uses the LocalStack Pro RDS service, provisioned by the
same Terraform workflow as DynamoDB. Export `LOCALSTACK_AUTH_TOKEN`, start
LocalStack with RDS and Secrets Manager enabled, then apply the local module:

```shell
export LOCALSTACK_AUTH_TOKEN="<your-token>"
docker compose up -d localstack
terraform -chdir=infrastructure/local apply
```

Use the Terraform endpoint and secret outputs to configure the `local-postgres`
Spring profile. Flyway applies the schema when the application starts. The
standalone `postgres` Compose service is intentionally not used; PostgreSQL
Testcontainers remains available for isolated automated tests.

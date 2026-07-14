# Student Portal API

A learning-oriented backend that first implements a university course registration portal with DynamoDB and then migrates the same application and data to PostgreSQL. The project makes database-specific tradeoffs visible instead of hiding them behind a misleadingly uniform persistence layer.

## Current checkpoint

The DynamoDB foundation now provisions six domain-oriented source tables and maps persistence-neutral domain objects to
separate Enhanced Client records. Profile-scoped clients and table bindings, conditional CRUD adapters, optimistic
locking, exact GSI lookups, and isolated LocalStack integration tests are in place. Cursor pagination over every
documented catalog and relationship GSI is also implemented through database-specific capability ports. Transactional
alternate-key claims and relationship-validating application services are now implemented for Departments, Students,
Profiles, Instructors, and Courses. Cross-table enrollment/capacity transactions, active locks, history counters, and
dependency-aware deletion are implemented. Controllers, seed data, and PostgreSQL remain later checkpoints.

## Repository layout

```text
.
├── student-portal-api/   Spring Boot application and Gradle wrapper
├── docs/                 Architecture, domain, and API decisions
├── infrastructure/       Terraform (introduced in the DynamoDB phase)
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

## Run the foundation application

```shell
cd student-portal-api
./gradlew bootRun
```

The initial health endpoint is available at `http://localhost:8080/actuator/health`.

## LocalStack Pro and DynamoDB infrastructure

Export your LocalStack token; do not place it in a committed file:

```shell
export LOCALSTACK_AUTH_TOKEN="<your-token>"
docker compose up -d localstack
```

Provision the six access-pattern-driven source tables:

```shell
terraform -chdir=infrastructure/local init
terraform -chdir=infrastructure/local validate
terraform -chdir=infrastructure/local plan
terraform -chdir=infrastructure/local apply
```

Run the application with the typed local configuration profile:

```shell
cd student-portal-api
./gradlew bootRun --args='--spring.profiles.active=local-dynamodb'
```

The profile reads `AWS_REGION`, `DYNAMODB_ENDPOINT`, and six table-name variables; safe local defaults are shown in
`.env.example`. The application does not create infrastructure on startup. The tables mirror domain boundaries for
migration learning, but reference attributes are not foreign keys and DynamoDB performs no joins.

To remove the local tables and services:

```shell
terraform -chdir=infrastructure/local destroy
docker compose down
```

If port `4566` is already in use, check for an existing LocalStack container before starting another. A healthy existing
LocalStack instance can be used by Terraform at the same endpoint; do not stop unrelated containers.

## Design documents

- [Architecture overview](docs/architecture-overview.md)
- [Domain model](docs/domain-model.md)
- [REST API contracts](docs/rest-api-contracts.md)
- [Implementation plan](docs/implementation-plan.md)
- [DynamoDB access patterns](docs/dynamodb-access-patterns.md)
- [DynamoDB table design](docs/dynamodb-table-design.md)
- [DynamoDB relationships](docs/dynamodb-relationships.md)
- [DynamoDB transactions](docs/dynamodb-transactions.md)

Seed data, Swagger UI, PostgreSQL profiles, migration commands, and reconciliation instructions will be added and
verified in their corresponding phases.

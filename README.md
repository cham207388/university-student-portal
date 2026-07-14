# Student Portal API

A learning-oriented backend that first implements a university course registration portal with DynamoDB and then migrates the same application and data to PostgreSQL. The project makes database-specific tradeoffs visible instead of hiding them behind a misleadingly uniform persistence layer.

## Current checkpoint

Phase 1 provides the persistence-neutral domain and REST contract foundation. It includes domain invariants,
validated immutable DTOs, response mapping, RFC 9457 Problem Details, pagination contracts, and repository ports.
It contains no DynamoDB or PostgreSQL persistence code.

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

## Run the foundation application

```shell
cd student-portal-api
./gradlew bootRun
```

The initial health endpoint is available at `http://localhost:8080/actuator/health`.

## Design documents

- [Architecture overview](docs/architecture-overview.md)
- [Domain model](docs/domain-model.md)
- [REST API contracts](docs/rest-api-contracts.md)
- [Implementation plan](docs/implementation-plan.md)

LocalStack, Terraform, database profiles, seed data, Swagger UI, migration commands, and reconciliation instructions will be added and verified in their corresponding phases.

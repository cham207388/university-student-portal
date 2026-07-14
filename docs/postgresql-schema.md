# PostgreSQL schema foundation (LocalStack RDS)

The development PostgreSQL engine is provisioned by LocalStack Pro's RDS
service. Terraform creates the instance and its Secrets Manager credentials;
the application connects to the resulting PostgreSQL endpoint over JDBC. This
keeps local provisioning close to the eventual AWS RDS workflow while still
using a real PostgreSQL engine. The standalone `postgres` Compose service is no
longer part of the supported setup.

Flyway migration `V1__initial_schema.sql` creates the six relational tables that
correspond to the DynamoDB aggregates: `departments`, `students`,
`student_profiles`, `instructors`, `courses`, and `enrollments`.

The schema deliberately keeps UUIDs and timestamps application-owned, while
PostgreSQL enforces foreign keys, alternate-key uniqueness, enum-like status
checks, non-negative counters, and the partial unique index that prevents two
active enrollments for one student/course pair. Profile deletion cascades from
its student; other relationships remain restrictive so dependency-aware
application services can return domain conflicts.

The course/instructor same-department rule and capacity/occupied-seat updates
remain application transaction responsibilities. The PostgreSQL adapter will
lock the course row (or use an atomic conditional update) while changing an
enrollment; this migration intentionally contains no JPA entities or triggers.

## Local runbook

1. Export `LOCALSTACK_AUTH_TOKEN` and start LocalStack with the `rds` and
   `secretsmanager` services enabled.
2. Apply the local Terraform module, which creates the RDS PostgreSQL instance
   and secret. Do not commit generated passwords or Terraform state.
3. Read the Terraform endpoint/secret outputs and export the corresponding
   `SPRING_DATASOURCE_*` values (or use the generated `.env` helper).
4. Start the application with `local-postgres`; Flyway applies migrations on
   startup.

The RDS emulator has different startup timing and endpoint behavior from AWS.
Wait for the instance status to become available before launching Spring Boot,
and use `make postgres-health` to verify TCP connectivity and Flyway readiness.
Testcontainers PostgreSQL remains the isolated CI integration path; it does not
depend on a developer's LocalStack state.

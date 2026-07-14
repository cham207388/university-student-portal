# AI Coding Agent Master Prompt: DynamoDB-to-PostgreSQL Migration Learning Project (Student Portal API) Master Prompt

You are an experienced Java 25, Spring Boot 4, Gradle 9, AWS DynamoDB, PostgreSQL, Spring Data JPA, Hibernate, Flyway, Terraform, LocalStack Pro, Docker Compose, Testcontainers, REST API, and database migration engineer.

Build a production-quality but learning-oriented backend application that demonstrates:

1. How to design a RESTful Java application using AWS DynamoDB.
2. How relational concepts are represented in DynamoDB without ORM relationships.
3. How to migrate the application from DynamoDB to PostgreSQL.
4. How to migrate existing data from DynamoDB into PostgreSQL.
5. How application architecture, repositories, transactions, queries, pagination, and data modeling change when moving from a NoSQL database to a relational database.

Do not merely replace dependencies. Explicitly demonstrate and document the architectural, modeling, querying, consistency, and transaction differences between DynamoDB and PostgreSQL.

⸻

## 1. Project Definition

Project name

student-portal-api (already created in current directory)

Java package

com.abc.studentportal

Technology stack

Use:

* Java 25
* Spring Boot 4.x, using the latest stable compatible release
* Gradle 9.x, using the Gradle Wrapper
* Gradle Kotlin DSL
* AWS SDK for Java 2.x
* DynamoDB Enhanced Client
* PostgreSQL
* Spring Data JPA
* Hibernate
* Flyway
* Terraform 1.14.x
* HashiCorp AWS Provider:
    * >= 6.50.0
    * < 7.0.0
* LocalStack Pro
* Docker Compose
* Testcontainers
* JUnit
* Mockito only where mocking provides value
* AssertJ
* Spring MVC
* Jakarta Bean Validation
* SpringDoc OpenAPI compatible with the selected Spring Boot version
* Jackson
* SLF4J and Logback
* Actuator health endpoints

Do not use:

* Maven
* Groovy Gradle scripts
* AWS SDK for Java 1.x
* DynamoDBMapper
* Deprecated Spring or AWS APIs
* MapStruct unless its benefit is clearly justified
* Spring Security
* A frontend
* A shared persistence entity model for both DynamoDB and PostgreSQL
* Automatic schema generation as a replacement for Flyway

Use Java records where appropriate for immutable API DTOs. Use conventional classes where frameworks require mutable objects or no-argument constructors. Use Lombok where it makes the code more readable.

⸻

## 2. Required Development Approach

Implement the project in discrete, verifiable phases.

Do not generate the entire project as an unexplained code dump.

For every phase:

1. Explain the objective.
2. List the files that will be created or modified.
3. Implement the code.
4. Run compilation and tests.
5. Fix failures before continuing.
6. Update documentation.
7. Create a Git commit with a meaningful message.
8. Summarize what changed and what was learned.

Never claim that a command, test, or build succeeded unless it was actually executed successfully.

Use small, focused commits. Do not combine the DynamoDB implementation and PostgreSQL migration into one commit.

⸻

## 3. Required Git Checkpoints

Use at least these commits:

- chore: initialize Java 25 Spring Boot 4 project
- feat: add core student portal domain and REST contracts
- infra: provision LocalStack DynamoDB with Terraform
- feat: implement DynamoDB persistence adapters
- feat: add DynamoDB API operations and access patterns
- test: add DynamoDB integration test suite
- docs: document DynamoDB architecture and data model
- feat: add PostgreSQL and Flyway infrastructure
- feat: implement PostgreSQL JPA persistence
- feat: switch application persistence to PostgreSQL
- feat: add DynamoDB to PostgreSQL migration job
- test: add migration reconciliation and parity tests
- docs: document migration decisions and lessons learned

- Create additional commits when useful.

- Before starting the PostgreSQL phase, create a Git tag: dynamodb-complete

- After completing the PostgreSQL migration, create: postgres-migration-complete

⸻

## 4. Architecture

Use a traditional layered architecture:

REST controller
    ↓
application service
    ↓
repository abstraction
    ↓
database-specific repository implementation
    ↓
DynamoDB or PostgreSQL

Use package-by-feature where practical, with clear layer boundaries.

A suitable high-level structure is:

src/main/java/com/abc/studentportal/
├── StudentPortalApiApplication.java
├── common/
│   ├── api/
│   ├── exception/
│   ├── pagination/
│   └── validation/
├── student/
│   ├── api/
│   ├── application/
│   ├── domain/
│   └── persistence/
├── department/
│   ├── api/
│   ├── application/
│   ├── domain/
│   └── persistence/
├── instructor/
│   ├── api/
│   ├── application/
│   ├── domain/
│   └── persistence/
├── course/
│   ├── api/
│   ├── application/
│   ├── domain/
│   └── persistence/
├── enrollment/
│   ├── api/
│   ├── application/
│   ├── domain/
│   └── persistence/
└── migration/
    ├── application/
    ├── configuration/
    ├── model/
    └── reporting/

The controller must not call AWS SDK clients or JPA repositories directly.

Business logic must live in application services, not controllers or persistence entities.

Database-specific annotations must not leak into REST request or response DTOs.

Avoid exposing JPA entities or DynamoDB records from API endpoints.

⸻

## 5. Domain Model

Build a University Course Registration Portal using these domain concepts.

Department

Fields:

* UUID id
* String code
* String name
* String description
* Instant createdAt
* Instant updatedAt
* long version

Rules:

* Department code is required and unique.
* Department code is uppercase.
* A department can have multiple instructors.
* A department can offer multiple courses.

Student

Fields:

* UUID id
* String studentNumber
* String firstName
* String lastName
* String email
* StudentStatus status
* UUID departmentId
* Instant createdAt
* Instant updatedAt
* long version

Rules:

* Student number is required and unique.
* Email is required, valid, and unique.
* A student belongs to one department.
* A department has many students.
* A student has exactly one student profile.
* A student can enroll in many courses.

Statuses:

ACTIVE
INACTIVE
GRADUATED
SUSPENDED

StudentProfile

Fields:

* UUID id
* UUID studentId
* LocalDate dateOfBirth
* String phoneNumber
* String addressLine1
* String addressLine2
* String city
* String state
* String postalCode
* String country
* Instant createdAt
* Instant updatedAt
* long version

Rules:

* A student profile belongs to exactly one student.
* A student can have no more than one profile.
* Deleting a student must delete or otherwise consistently remove the profile according to the documented domain policy.

Instructor

Fields:

* UUID id
* String employeeNumber
* String firstName
* String lastName
* String email
* UUID departmentId
* Instant createdAt
* Instant updatedAt
* long version

Rules:

* Employee number is unique.
* Email is unique.
* An instructor belongs to one department.
* An instructor may teach many courses.
* Each course has one primary instructor.

Course

Fields:

* UUID id
* String courseCode
* String title
* String description
* int credits
* int capacity
* CourseStatus status
* UUID departmentId
* UUID instructorId
* Instant createdAt
* Instant updatedAt
* long version

Rules:

* Course code is unique.
* Credits must be positive.
* Capacity must be positive.
* A course belongs to one department.
* A course has one instructor.
* An instructor teaches multiple courses.
* A course has many students through enrollments.

Statuses:

DRAFT
OPEN
CLOSED
CANCELLED
COMPLETED

Enrollment

Enrollment is an explicit association entity between Student and Course.

Fields:

* UUID id
* UUID studentId
* UUID courseId
* EnrollmentStatus status
* Instant enrolledAt
* Instant droppedAt
* String finalGrade
* Instant createdAt
* Instant updatedAt
* long version

Statuses:

ENROLLED
WAITLISTED
DROPPED
COMPLETED

Rules:

* A student cannot have duplicate active enrollment in the same course.
* A student cannot enroll in a cancelled, closed, or completed course.
* Enrollment must not exceed course capacity.
* A dropped enrollment does not consume capacity.
* A completed enrollment may have a final grade.
* A final grade is invalid for an enrollment that is not completed.
* Enrollment and available capacity must remain consistent.
* Concurrent enrollment attempts must not overbook a course.

⸻

## 6. Relationship Demonstration

The application must explicitly demonstrate all four requested relationship types.

One-to-one

Student ↔ StudentProfile

One-to-many

Department → Students
Department → Courses
Department → Instructors
Instructor → Courses

Many-to-one

Student → Department
Course → Department
Instructor → Department
Course → Instructor

Many-to-many

Student ↔ Course

Represent the many-to-many relationship using the explicit Enrollment association entity.

Do not describe DynamoDB references as JPA relationships. DynamoDB has no foreign keys, joins, or ORM relationship annotations. Explain how these logical relationships are implemented through keys, denormalized attributes, access patterns, and application-level integrity checks.

⸻

## 7. REST API Requirements

Use /api/v1 as the base path.

Use proper HTTP methods and status codes.

Use DTOs for all requests and responses.

Use UUIDs as externally visible identifiers.

Department endpoints

POST   /api/v1/departments
GET    /api/v1/departments/{departmentId}
GET    /api/v1/departments
PUT    /api/v1/departments/{departmentId}
DELETE /api/v1/departments/{departmentId}
GET    /api/v1/departments/{departmentId}/students
GET    /api/v1/departments/{departmentId}/instructors
GET    /api/v1/departments/{departmentId}/courses

Student endpoints

POST   /api/v1/students
GET    /api/v1/students/{studentId}
GET    /api/v1/students
PUT    /api/v1/students/{studentId}
PATCH  /api/v1/students/{studentId}/status
DELETE /api/v1/students/{studentId}
PUT    /api/v1/students/{studentId}/profile
GET    /api/v1/students/{studentId}/profile
DELETE /api/v1/students/{studentId}/profile
GET    /api/v1/students/{studentId}/enrollments
GET    /api/v1/students/{studentId}/courses

Instructor endpoints

POST   /api/v1/instructors
GET    /api/v1/instructors/{instructorId}
GET    /api/v1/instructors
PUT    /api/v1/instructors/{instructorId}
DELETE /api/v1/instructors/{instructorId}
GET    /api/v1/instructors/{instructorId}/courses

Course endpoints

POST   /api/v1/courses
GET    /api/v1/courses/{courseId}
GET    /api/v1/courses
PUT    /api/v1/courses/{courseId}
PATCH  /api/v1/courses/{courseId}/status
DELETE /api/v1/courses/{courseId}
GET    /api/v1/courses/{courseId}/students
GET    /api/v1/courses/{courseId}/enrollments

Enrollment endpoints

POST   /api/v1/enrollments
GET    /api/v1/enrollments/{enrollmentId}
GET    /api/v1/enrollments
PATCH  /api/v1/enrollments/{enrollmentId}/status
DELETE /api/v1/enrollments/{enrollmentId}

Use DELETE carefully. Document whether deletion is physical deletion or a domain-level status transition for each resource.

⸻

8. API Features

Implement all of the following.

CRUD

Provide complete create, retrieve, update, and delete behavior where appropriate.

Pagination

For PostgreSQL, support conventional page-based pagination:

?page=0&size=20

Return metadata such as:

{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "hasNext": false,
  "hasPrevious": false
}

For DynamoDB, do not emulate offset pagination internally.

Use cursor-based pagination based on DynamoDB LastEvaluatedKey.

Example:

?limit=20&cursor=<opaque-encoded-cursor>

Return:

{
  "content": [],
  "limit": 20,
  "nextCursor": null,
  "hasNext": false
}

Explain why DynamoDB cannot efficiently provide PostgreSQL-style total counts and arbitrary page offsets.

If maintaining a common API contract requires compromises, document them explicitly. Do not hide inefficient table scans behind a repository abstraction.

Sorting

Support only sorting that can be implemented efficiently for each database.

PostgreSQL may support validated dynamic sorting.

DynamoDB sorting must follow partition and sort key design. Reject unsupported sort requests rather than silently scanning and sorting large datasets in application memory.

Search and filtering

Include practical filters such as:

Students:

departmentId
status
email
studentNumber
lastName

Courses:

departmentId
instructorId
status
courseCode
title
minimumCredits
maximumCredits

Enrollments:

studentId
courseId
status
enrolledFrom
enrolledTo

In the DynamoDB implementation:

* Design GSIs around required access patterns.
* Distinguish Query from Scan.
* Avoid scans for primary business endpoints.
* Where a substring search cannot be efficiently supported, document or reject it.
* Never imply that DynamoDB supports arbitrary SQL-like filtering efficiently.

In PostgreSQL:

* Implement appropriate repository queries.
* Add indexes that support actual filters and joins.
* Use case-insensitive matching where applicable.
* Avoid unnecessary N+1 queries.

Validation

Use Jakarta Bean Validation.

Include:

* Required-field checks
* Length constraints
* Email validation
* Positive numeric validation
* Enum validation
* Cross-field domain validation
* Duplicate checks
* Relationship existence checks

Return field-level validation details.

Global exception handling

Use @RestControllerAdvice.

Use RFC 9457-compatible Problem Details where supported.

At minimum, handle:

* Resource not found
* Duplicate resource
* Invalid relationship
* Domain rule violation
* Validation failure
* Optimistic locking conflict
* Conditional DynamoDB write failure
* PostgreSQL constraint violation
* Malformed request
* Invalid cursor
* Unsupported sorting or filter operation
* Unexpected internal failure

Do not expose stack traces or internal implementation details to API consumers.

Transactions

Demonstrate equivalent business operations using each database’s transaction capabilities.

At minimum, implement transactional enrollment:

1. Verify student exists.
2. Verify course exists.
3. Verify course is open.
4. Verify no duplicate active enrollment exists.
5. Verify capacity remains.
6. Create enrollment.
7. Update enrollment count or capacity control record when required by the DynamoDB model.

For DynamoDB:

* Use condition expressions.
* Use TransactWriteItems or the Enhanced Client transaction APIs.
* Prevent race conditions and overbooking.
* Implement optimistic concurrency where appropriate.

For PostgreSQL:

* Use @Transactional.
* Use database constraints.
* Use an appropriate concurrency strategy, such as optimistic locking, pessimistic locking, or an atomic conditional update.
* Explain the selected isolation and locking behavior.
* Prove with concurrent integration tests that capacity cannot be exceeded.

OpenAPI

Provide:

* Swagger UI
* OpenAPI JSON
* Endpoint descriptions
* Request and response schemas
* Validation constraints
* Error response schemas
* Pagination documentation
* Example requests and responses
* Explanation of DynamoDB cursor pagination

⸻

9. Phase One: DynamoDB Implementation

Build and complete the DynamoDB application before adding PostgreSQL.

DynamoDB modeling requirement

Do not begin by creating one table per Java entity without analysis.

First create:

docs/dynamodb-access-patterns.md

List every required access pattern before choosing keys.

For each access pattern, record:

* Operation name
* Input parameters
* Expected result
* Cardinality
* Required ordering
* Consistency requirement
* Candidate partition key
* Candidate sort key
* GSI requirement
* Whether transaction support is required

Examples:

Get student by ID
Get student by student number
Get student by email
List students by department
List students by status
Get profile by student
List courses by department
List courses by instructor
Get course by course code
List enrollments by student
List enrollments by course
Determine whether a student is actively enrolled in a course
Count active enrollments for a course
Enroll a student without exceeding capacity

Table design

Prefer a well-reasoned single-table design unless a small number of tables is demonstrably clearer for this learning project.

The final choice must be documented.

A possible single-table pattern may use generic attributes:

PK
SK
GSI1PK
GSI1SK
GSI2PK
GSI2SK
entityType
payload attributes
createdAt
updatedAt
version

Possible item keys:

PK=STUDENT#<studentId>
SK=METADATA
PK=STUDENT#<studentId>
SK=PROFILE
PK=STUDENT#<studentId>
SK=ENROLLMENT#<courseId>
PK=COURSE#<courseId>
SK=METADATA
PK=COURSE#<courseId>
SK=ENROLLMENT#<studentId>
PK=DEPARTMENT#<departmentId>
SK=METADATA
PK=INSTRUCTOR#<instructorId>
SK=METADATA

This is only a starting point. Validate it against every access pattern.

If an enrollment is represented by multiple denormalized items, use transactional writes and define ownership, consistency, and repair behavior.

DynamoDB requirements

Use:

* AWS SDK for Java 2.x
* DynamoDB Enhanced Client
* Explicit table schemas
* Conditional writes
* Transactional writes
* Cursor pagination
* GSIs for required alternate access patterns
* Consistent reads only where justified
* version for optimistic locking or an equivalent explicit condition strategy
* ISO-8601 timestamps
* UUID identifiers

Do not use:

* DynamoDBMapper
* AWS SDK v1
* JPA annotations in DynamoDB persistence classes
* Full-table scans for normal entity retrieval
* In-memory joins across unbounded datasets
* Unbounded result collections
* Fake foreign keys
* Unsupported assumptions about cascades

Referential integrity

Because DynamoDB has no foreign keys, application services must enforce relationship integrity.

Document race conditions that application-level checks alone cannot eliminate and show where transactional conditions are used.

Denormalization

Document:

* Which data is duplicated
* Why it is duplicated
* Which copy is authoritative
* How updates propagate
* How partial failure is prevented
* How inconsistencies can be detected and repaired

⸻

## 10. LocalStack Pro and Terraform

Use Docker Compose to run LocalStack Pro.

Use Terraform to provision AWS-emulated infrastructure. Do not create tables imperatively from application startup code.

Docker Compose

Create:

compose.yaml
.env.example

The composition must eventually support:

* LocalStack Pro
* PostgreSQL
* Optional database administration UI only if it does not distract from the learning objective

Do not commit secrets or a LocalStack auth token.

Use an environment variable:

LOCALSTACK_AUTH_TOKEN

Add health checks.

Use named volumes where persistence is useful.

Use a dedicated Docker network.

Terraform structure

Create:

infrastructure/
├── local/
│   ├── versions.tf
│   ├── providers.tf
│   ├── variables.tf
│   ├── locals.tf
│   ├── dynamodb.tf
│   ├── outputs.tf
│   └── terraform.tfvars.example
└── modules/
    └── dynamodb/
        ├── main.tf
        ├── variables.tf
        └── outputs.tf

Terraform requirements:

terraform {
  required_version = "~> 1.14.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 6.50.0, < 7.0.0"
    }
  }
}

Configure the AWS provider for LocalStack using explicit service endpoints and dummy credentials.

Use safe local settings such as:

skip_credentials_validation
skip_metadata_api_check
skip_requesting_account_id

Configure only settings supported by the selected provider version.

Provision:

* DynamoDB table or tables
* Partition and sort keys
* GSIs
* Billing mode suitable for local development
* Tags
* Outputs consumed by local configuration

Commit:

.terraform.lock.hcl

Do not commit:

.terraform/
terraform.tfstate
terraform.tfstate.backup
*.tfvars containing secrets

Provide commands for:

docker compose up -d
terraform init
terraform validate
terraform plan
terraform apply
terraform destroy

⸻

## 11. Application Configuration

Use Spring profiles:

local-dynamodb
local-postgres
test-dynamodb
test-postgres
migration

During the initial phase, local-dynamodb is the primary profile.

Externalize:

* AWS region
* DynamoDB endpoint
* Table names
* PostgreSQL URL
* PostgreSQL username
* PostgreSQL password
* Migration batch size
* Migration retry limits

Use typed @ConfigurationProperties.

Fail fast for missing required configuration.

Do not hardcode LocalStack endpoints in production-oriented classes.

⸻

## 12. DynamoDB Testing

Implement:

Unit tests

Test:

* Business validation
* Domain rules
* Cursor encoding and decoding
* DTO mapping
* Service behavior where isolated tests add value

Avoid mocking the DynamoDB Enhanced Client extensively. Prefer integration tests for persistence behavior.

Integration tests

Use Testcontainers with LocalStack.

Tests must provision the same table structure used by Terraform or reuse shared infrastructure definitions in a maintainable manner.

Test:

* CRUD behavior
* Every required GSI query
* Cursor pagination
* Conditional updates
* Optimistic concurrency
* Transaction rollback
* Duplicate enrollment prevention
* Concurrent capacity enforcement
* Denormalized item consistency
* Delete behavior
* Error translation
* REST endpoints

Do not rely on a developer’s manually running LocalStack instance for automated tests.

⸻

## 13. DynamoDB Documentation Checkpoint

Before starting PostgreSQL, produce:

docs/
├── architecture-dynamodb.md
├── dynamodb-access-patterns.md
├── dynamodb-table-design.md
├── dynamodb-relationships.md
├── dynamodb-transactions.md
└── dynamodb-limitations.md

Include Mermaid diagrams for:

* Application architecture
* Item collection layout
* Main access patterns
* Transactional enrollment flow

Document:

* Why DynamoDB has no JPA-style mappings
* How each logical relationship is represented
* Query versus scan
* Cursor pagination
* Denormalization
* Eventual versus strong consistency
* Transaction limits
* Referential integrity limitations
* Operational tradeoffs
* What must change during migration

Run all tests and tag the repository:

git tag dynamodb-complete

⸻

## 14. Phase Two: Introduce PostgreSQL

After the DynamoDB checkpoint, add PostgreSQL to Docker Compose.

Do not delete the DynamoDB implementation yet. It is needed as the migration source and as a comparison baseline.

Use a supported PostgreSQL version, preferably PostgreSQL 17 or newer if all selected libraries and Testcontainers support it.

Configure:

* Named volume
* Health check
* Database
* Application user
* Credentials through environment variables
* No production secrets committed to Git

Use Testcontainers PostgreSQL for automated integration tests.

⸻

## 15. PostgreSQL Relational Model

Create database tables for:

departments
students
student_profiles
instructors
courses
enrollments

Required relational mappings

Implement:

One-to-one

StudentEntity ↔ StudentProfileEntity

One-to-many and many-to-one

DepartmentEntity ↔ StudentEntity
DepartmentEntity ↔ InstructorEntity
DepartmentEntity ↔ CourseEntity
InstructorEntity ↔ CourseEntity

Many-to-many through association entity

StudentEntity ↔ EnrollmentEntity ↔ CourseEntity

Do not use a direct @ManyToMany collection with an implicit join table. Enrollment contains domain fields and must remain an explicit entity.

JPA guidance

Use:

* Explicit table and column names
* Appropriate fetch strategies
* @Version for optimistic locking
* Database constraints
* Unique constraints
* Foreign keys
* Indexes
* Auditing timestamps
* Explicit cascade and orphan-removal decisions
* Entity graphs, projections, or fetch joins where justified
* DTO mapping outside controllers

Avoid:

* FetchType.EAGER by default
* Bidirectional relationships without a demonstrated need
* Returning entities from controllers
* CascadeType.ALL everywhere
* Open Session in View
* Accidental N+1 queries
* Hibernate schema auto-generation in production
* Equality methods that traverse relationships
* Including lazy collections in toString

Set:

spring.jpa.open-in-view=false

Use Hibernate validation against the Flyway-managed schema where useful, but Flyway remains the schema authority.

⸻

## 16. Flyway

Create versioned migrations such as:

V1__create_departments.sql
V2__create_students_and_profiles.sql
V3__create_instructors_and_courses.sql
V4__create_enrollments.sql
V5__create_indexes.sql
V6__add_migration_tracking.sql

You may consolidate migrations when sensible, but maintain clear progression.

Include:

* Primary keys
* Foreign keys
* Unique constraints
* Check constraints
* Not-null constraints
* Indexes supporting actual filters
* Version columns
* Timestamp columns
* Enrollment uniqueness rules
* Grade/status consistency where practical

Use database constraints as a final integrity boundary, while preserving clear application-level validation messages.

Do not modify an already-applied migration. Add a new migration for changes.

⸻

## 17. PostgreSQL Repository Implementation

Implement persistence using Spring Data JPA.

Retain application service behavior and REST contracts wherever practical.

Replace DynamoDB-specific repository adapters with PostgreSQL adapters.

Do not force one repository interface to pretend that DynamoDB and PostgreSQL have identical capabilities.

Separate:

* Common business operations
* DynamoDB-specific cursor operations
* PostgreSQL-specific pageable operations
* Migration source reads
* Migration target writes

Document any API changes required by database capability differences.

Implement efficient filtering with:

* Derived queries where simple
* JPQL where clear
* Specifications or Criteria API for composable filters
* Native SQL only where it provides a justified advantage

Add integration tests that detect N+1 problems for important aggregate endpoints when practical.

⸻

## 18. Transactional PostgreSQL Enrollment

Implement enrollment as a real relational transaction.

The operation must:

1. Lock or safely update the relevant course capacity state.
2. Verify course eligibility.
3. Check for duplicate enrollment.
4. Insert the enrollment.
5. Commit atomically.
6. Roll back all changes on failure.

Choose and explain one concurrency approach:

* Pessimistic row locking
* Optimistic locking with bounded retry
* Atomic conditional SQL update

The test suite must issue concurrent enrollment requests and prove:

* Capacity is never exceeded.
* Duplicate active enrollment is not created.
* Failed transactions do not leave partial state.
* Correct HTTP conflicts are returned.

Compare this design directly with DynamoDB transactional writes and condition expressions.

⸻

## 19. Actual DynamoDB-to-PostgreSQL Data Migration

Build a real migration component. Do not use only a README or pseudocode.

Run it using the migration Spring profile.

The migration process must read from DynamoDB and write to PostgreSQL.

Migration execution

Support an explicit command such as:

./gradlew bootRun --args='--spring.profiles.active=migration --migration.run=true'

The migration must not run accidentally during normal API startup.

Require an explicit enablement property:

migration.run=true

Migration order

Migrate in referentially safe order:

1. Departments
2. Students
3. Student profiles
4. Instructors
5. Courses
6. Enrollments

Adapt order if the final schema requires it.

Migration behavior

Implement:

* Configurable batch size
* DynamoDB pagination
* PostgreSQL batch writes where safe
* Idempotent upserts or insert-if-absent behavior
* Restartability
* Checkpointing
* Retry with bounded exponential backoff
* Clear failure logging
* Dead-letter or rejected-record reporting
* Per-entity counters
* Start and end timestamps
* Dry-run mode
* Referential validation
* Duplicate detection
* Data transformation
* Version and timestamp preservation
* Deterministic handling of denormalized DynamoDB records
* Transaction boundaries appropriate to each batch

Do not wrap the entire migration in one database transaction.

Do not silently skip bad records.

Migration tracking

Create PostgreSQL tables such as:

migration_runs
migration_checkpoints
migration_errors

A migration run should track:

* Run ID
* Status
* Start time
* End time
* Source
* Target
* Entity type
* Read count
* Insert count
* Update count
* Skip count
* Failure count
* Last processed key or checkpoint
* Configuration
* Error summary

Use statuses such as:

STARTED
IN_PROGRESS
COMPLETED
COMPLETED_WITH_ERRORS
FAILED
CANCELLED

Idempotency

Running the migration twice must not duplicate data.

Define the business and technical keys used for idempotency.

Use deterministic identifiers or source identifiers where appropriate.

Data transformation

Create explicit source-to-target mappers.

Do not annotate DynamoDB persistence records as JPA entities.

Handle:

* Denormalized enrollment copies
* Duplicate representations
* Missing optional fields
* Enum conversion
* Timestamp conversion
* Invalid references
* Legacy or malformed records
* Version mapping

Identify the authoritative DynamoDB item for each target row.

⸻

## 20. Migration Reconciliation

After migration, automatically reconcile DynamoDB and PostgreSQL.

Generate a report containing:

* Source item counts by entity
* Target row counts by entity
* Distinct logical entity counts
* Duplicate counts
* Missing source IDs
* Unexpected target IDs
* Invalid relationships
* Field-level mismatches
* Enrollment count mismatches
* Course capacity inconsistencies
* Checksum or fingerprint mismatches
* Migration errors
* Overall pass/fail result

Because DynamoDB may contain multiple physical items for one logical entity, distinguish:

physical source item count
logical source entity count
target relational row count

Generate reports in:

build/reports/migration/

Use at least:

migration-summary.json
migration-summary.md
migration-errors.csv

Do not include sensitive data in reports.

Exit with a nonzero status when reconciliation fails beyond configurable thresholds.

⸻

## 21. Dual-Read Verification Mode

Add an optional verification mode for learning purposes.

In this mode:

1. PostgreSQL is the primary read source.
2. The application reads the equivalent logical record from DynamoDB.
3. Results are normalized.
4. Differences are logged or recorded.
5. The PostgreSQL response is returned to the caller.

Do not enable this mode by default.

Do not place dual-read logic in controllers.

Add properties such as:

verification.dual-read.enabled=false
verification.sample-rate=0.10
verification.fail-request-on-mismatch=false

Document that this is a temporary migration verification mechanism, not a permanent architecture.

Avoid exposing personal fields in mismatch logs.

⸻

## 22. Persistence Switching

Initially use:

local-dynamodb

After migration, use:

local-postgres

The final normal application runtime must use PostgreSQL as its primary database.

DynamoDB code may remain under a migration or legacy adapter package so the migration and comparison tests can run.

Do not create ambiguous bean selection. Use profiles and explicit configuration.

Document the exact configuration changes required to switch persistence implementations.

⸻

## 23. API Compatibility and Contract Testing

Create contract-level tests that run equivalent API scenarios against both persistence implementations.

Verify parity for:

* Resource creation
* Resource retrieval
* Updates
* Validation failures
* Duplicate detection
* Relationship retrieval
* Enrollment rules
* Filtering
* Supported sorting
* Error response structure

Where pagination semantics differ, explicitly document and test the difference instead of pretending the contracts are identical.

Create:

docs/api-compatibility-matrix.md

Use columns such as:

Capability
DynamoDB behavior
PostgreSQL behavior
API impact
Migration decision

⸻

## 24. Required Test Coverage

The completed project must include:

Domain and service tests

* Student validation
* Profile uniqueness
* Department membership
* Course capacity
* Course status transitions
* Enrollment status transitions
* Grade validation
* Duplicate enrollment
* Deletion policies

DynamoDB integration tests

* Key design
* GSI queries
* Conditional writes
* Transactions
* Pagination
* Optimistic concurrency
* Denormalized record consistency

PostgreSQL integration tests

* Flyway startup
* CRUD
* Foreign keys
* Unique constraints
* JPA mappings
* Filtering
* Sorting
* Pagination
* Transaction rollback
* Optimistic locking
* Concurrent enrollment
* No critical N+1 behavior

Migration integration tests

Create source data in LocalStack DynamoDB, run the migration, and verify PostgreSQL.

Include:

* Normal migration
* Empty source
* Multiple pages
* Rerun idempotency
* Resume from checkpoint
* Duplicate DynamoDB representations
* Missing relationship
* Invalid enum
* Partial batch failure
* Retryable failure
* Dry run
* Reconciliation failure
* Successful reconciliation

Use a test environment containing both LocalStack and PostgreSQL Testcontainers.

⸻

## 25. Seed Data

Provide a controlled development data seeder that creates:

* At least 3 departments
* At least 10 students
* Profiles for students
* At least 5 instructors
* At least 10 courses
* Multiple enrollments
* At least one full course
* Different student, course, and enrollment statuses

Seeding must be:

* Explicitly enabled
* Idempotent
* Disabled in normal production configuration
* Available for both DynamoDB and PostgreSQL

Use realistic but fictional data.

⸻

## 26. Observability and Operations

Add:

* Structured and contextual logging
* Correlation ID support
* Actuator health endpoint
* DynamoDB health indicator
* PostgreSQL health indicator
* Migration progress logs
* Migration counters
* Startup configuration summary without secrets

Never log:

* Database passwords
* LocalStack token
* Full student profiles
* Sensitive personal data

Add metrics if practical for:

* API request timing
* DynamoDB operations
* PostgreSQL operations
* Migration records processed
* Migration errors
* Reconciliation mismatches

⸻

## 27. Gradle Requirements

Use:

settings.gradle.kts
build.gradle.kts
gradle/libs.versions.toml

Use a Java toolchain:

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

Use the Gradle Wrapper.

Organize dependencies through the version catalog where appropriate.

Create useful tasks such as:

test
integrationTest
dynamodbIntegrationTest
postgresIntegrationTest
migrationIntegrationTest
check
bootRun

Ensure:

./gradlew clean check

runs the required verification suite or clearly document any separated test tasks.

Use test tags or source sets to keep test intent clear.

Do not use dynamic dependency versions such as 1.+ or latest.release.

⸻

## 28. Code Quality Requirements

Use:

* Clear names
* Small methods
* Constructor injection
* Immutable DTOs
* Explicit mapping
* Centralized clock abstraction where timestamp testing benefits
* Consistent error responses
* Defensive validation
* Bounded collection retrieval
* Appropriate database indexes
* Comments only where they explain non-obvious decisions

Avoid:

* Field injection
* Static mutable state
* God services
* Generic catch (Exception) without appropriate handling
* Repository calls directly from controllers
* Business logic in persistence entities
* Excessive inheritance
* Copy-pasted mapping logic
* N+1 query patterns
* Accidental DynamoDB scans
* Silent data loss during migration

Run formatting and static analysis if compatible tools are available for Java 25 and Spring Boot 4.

⸻

## 29. Documentation

Create a comprehensive root README.md containing:

* Project objective
* Architecture
* Prerequisites
* Java and Gradle versions
* LocalStack Pro configuration
* Docker Compose commands
* Terraform commands
* DynamoDB startup instructions
* PostgreSQL startup instructions
* Application profiles
* Running tests
* Seeding data
* Swagger UI location
* Example API calls
* Running the migration
* Dry-run migration
* Resuming a migration
* Reading reconciliation reports
* Switching persistence implementations
* Troubleshooting
* Git checkpoint explanation

Also create:

docs/
├── architecture-overview.md
├── architecture-dynamodb.md
├── architecture-postgres.md
├── dynamodb-access-patterns.md
├── dynamodb-table-design.md
├── dynamodb-relationships.md
├── dynamodb-transactions.md
├── dynamodb-limitations.md
├── postgres-relational-model.md
├── postgres-transactions.md
├── migration-design.md
├── migration-runbook.md
├── migration-reconciliation.md
├── api-compatibility-matrix.md
├── data-model-comparison.md
├── performance-comparison.md
└── lessons-learned.md

Use Mermaid diagrams for:

* Domain relationships
* DynamoDB physical item model
* PostgreSQL ERD
* Layered architecture
* Enrollment transaction
* Migration flow
* Reconciliation flow
* Runtime profile switching

⸻

## 30. Required Comparison Analysis

In docs/data-model-comparison.md, compare:

Concern	DynamoDB	PostgreSQL
Entity identity		
Relationships		
Referential integrity		
One-to-one		
One-to-many		
Many-to-many		
Query flexibility		
Pagination		
Sorting		
Transactions		
Concurrency		
Uniqueness		
Schema evolution		
Denormalization		
Indexes		
Delete behavior		
Operational scaling		
Cost model		
Migration complexity		

Include concrete examples from this project rather than generic database definitions.

⸻

## 31. Migration Decision Record

Create architecture decision records:

docs/adr/
├── 0001-use-layered-architecture.md
├── 0002-use-dynamodb-enhanced-client.md
├── 0003-dynamodb-table-design.md
├── 0004-use-explicit-enrollment-entity.md
├── 0005-use-postgresql-and-jpa.md
├── 0006-use-flyway.md
├── 0007-migration-idempotency-strategy.md
└── 0008-enrollment-concurrency-strategy.md

Each ADR must include:

* Context
* Decision
* Alternatives considered
* Consequences
* Risks
* Validation approach

⸻

## 32. Example Developer Workflow

Create a Makefile for the project to simplify the developer workflow.

The finished project should support a workflow similar to:

Start LocalStack

export LOCALSTACK_AUTH_TOKEN="<your-token>"
docker compose up -d localstack

Provision DynamoDB

cd infrastructure/local
terraform init
terraform validate
terraform plan
terraform apply

Run DynamoDB application

./gradlew bootRun \
  --args='--spring.profiles.active=local-dynamodb'

Run DynamoDB tests

./gradlew dynamodbIntegrationTest

Start PostgreSQL

docker compose up -d postgres

Run PostgreSQL application

./gradlew bootRun \
  --args='--spring.profiles.active=local-postgres'

Run PostgreSQL tests

./gradlew postgresIntegrationTest

Dry-run migration

./gradlew bootRun \
  --args='--spring.profiles.active=migration --migration.run=true --migration.dry-run=true'

Execute migration

./gradlew bootRun \
  --args='--spring.profiles.active=migration --migration.run=true --migration.dry-run=false'

Run migration tests

./gradlew migrationIntegrationTest

Run all verification

./gradlew clean check

Adjust commands to match the actual implementation and verify each command.

⸻

## 33. Definition of Done

The project is complete only when:

* The application compiles with Java 25.
* The Gradle Wrapper uses a compatible Gradle 9 release.
* The DynamoDB implementation is fully functional.
* Terraform provisions DynamoDB in LocalStack Pro.
* DynamoDB APIs support the designed access patterns.
* DynamoDB scans are not used for core endpoints without explicit justification.
* DynamoDB integration tests pass.
* The DynamoDB checkpoint is committed and tagged.
* PostgreSQL starts through Docker Compose.
* Flyway creates the complete relational schema.
* JPA relationships and repositories work correctly.
* PostgreSQL integration tests pass.
* Transactional enrollment cannot exceed capacity under concurrency.
* The actual DynamoDB-to-PostgreSQL migration runs.
* Migration reruns are idempotent.
* Migration can resume from a checkpoint.
* Reconciliation reports are generated.
* Contract parity tests pass or documented differences are proven.
* OpenAPI documentation is available.
* Global exception handling is consistent.
* No authentication is included.
* No secrets are committed.
* All documentation is accurate.
* ./gradlew clean check succeeds.
* The PostgreSQL migration checkpoint is committed and tagged.

⸻

## 34. Agent Operating Rules

Follow these rules throughout the implementation:

1. Inspect the existing repository before modifying it.
2. Preserve working code unless a migration step requires a deliberate replacement.
3. Never skip a failing test to make the build green.
4. Never disable validation merely to pass a test.
5. Never use placeholder pseudocode in production paths.
6. Never report success without executing the relevant command.
7. Use official documentation when resolving version or API compatibility questions.
8. Keep dependency versions compatible with Java 25, Spring Boot 4, and Gradle 9.
9. Explain material DynamoDB and PostgreSQL tradeoffs as they arise.
10. Highlight every place where relational assumptions fail in DynamoDB.
11. Highlight every place where the PostgreSQL design becomes simpler or more constrained.
12. Prefer correctness and clarity over excessive abstraction.
13. Keep controllers thin.
14. Keep persistence details out of API DTOs.
15. Protect against concurrent over-enrollment in both databases.
16. Preserve source identifiers and timestamps during migration.
17. Keep migration execution explicit and restartable.
18. Never silently discard malformed or inconsistent source data.
19. Update the README and technical documentation at every major checkpoint.
20. Stop at the end of each major phase and provide a concise implementation report before proceeding.

⸻

## 35. Start Here

Begin with Phase 0 only:

1. Inspect the current working directory.
2. Confirm whether the repository is empty or already initialized.
3. Verify installed Java, Gradle, Docker, Terraform, and Git versions.
4. Design the domain model.
5. Define the REST contracts.
6. Create the initial architecture and implementation plan.
7. Create the Spring Boot project skeleton.
8. Configure Java 25 and Gradle 9 Kotlin DSL.
9. Add only the dependencies required for the initial project foundation.
10. Add an initial health endpoint and context-load test.
11. Run the build and tests.
12. Create the first Git commit:

chore: initialize Java 25 Spring Boot 4 project

After Phase 0, report:

* Files created
* Commands executed
* Test results
* Architecture decisions
* Risks or compatibility issues
* Git commit hash
* Exact proposed scope for Phase 1

Do not implement DynamoDB or PostgreSQL persistence during Phase 0.
# ADR 0009: Use multiple DynamoDB source tables

## Context

The original design selected a single table to teach access-pattern-driven DynamoDB modeling. The project’s primary
practical objective was clarified after reviewing `reference-project-data.md`: prepare for migrating an existing estate
of many DynamoDB tables with different keys, indexes, embedded structures, logical joins, legacy fields, and data-quality
issues into PostgreSQL.

## Decision

Use six DynamoDB tables aligned to the Student Portal’s source entities: Departments, Students, StudentProfiles,
Instructors, Courses, and Enrollments. Keep database-specific records separate from the domain and eventual JPA entities.
Relationships remain application-enforced references. Cross-entity enrollment consistency uses DynamoDB transactions
across the Student, Course, and Enrollment tables. The Enrollment table also owns typed active-pair locks and one
durable, deterministic edge per distinct Student/Course pair for deduplicated relationship navigation.

## Alternatives considered

- Retain only the single-table design: strong DynamoDB modeling lesson, but less representative of the target migration.
- Implement both designs at runtime: maximizes breadth but doubles adapter, infrastructure, and test scope while
  distracting from the end-to-end migration.
- Use one table per aggregate plus additional lock/counter tables: operationally explicit, but unnecessary because the
  enrollment table can hold typed lock records and the course record can own its counter.

## Consequences

Migration now requires referential ordering, six independently paginated readers, per-table checkpoints, and cross-table
reconciliation. Relationship APIs use GSIs rather than co-located item collections. Infrastructure has more resources
and indexes. The project still teaches access-pattern-first design within each table and explicitly documents why GSIs
do not provide uniqueness or foreign keys.

## Risks

The model can look deceptively relational even though DynamoDB supplies no joins or referential constraints. Cross-table
transactions have action/size limits. GSI propagation is eventually consistent. Data migration must distinguish typed
enrollment lock and relationship-edge records from logical enrollments and must not assume all source references are
valid.

## Validation approach

Terraform must create exactly six tables with the documented keys and indexes. Integration tests will cover every GSI,
cross-table enrollment transactions, stale versions, uniqueness races, orphans, migration order, per-table resume,
legacy aliases, invalid enums, and physical-versus-logical reconciliation counts.

# DynamoDB table design

## Decision

Use one table named `student-portal` with string keys `PK` and `SK` and four sparse GSIs. A single table keeps each
bounded aggregate and relationship collection queryable without joins while making transactional ownership explicit.
It is not a relational schema translated into six tables.

| Index | Partition key | Sort key | Purpose |
| --- | --- | --- | --- |
| Table | `PK` | `SK` | Entity lookup, aggregate collections, claims, and capacity state |
| `gsi1-entity-catalog` | `GSI1PK` | `GSI1SK` | Bounded all-entity lists and migration authoritative-item iteration alternative |
| `gsi2-department-members` | `GSI2PK` | `GSI2SK` | Students, instructors, and courses by department |
| `gsi3-entity-status` | `GSI3PK` | `GSI3SK` | Students, courses, and authoritative enrollments by status |
| `gsi4-instructor-courses` | `GSI4PK` | `GSI4SK` | Courses by primary instructor |

All GSIs use `ALL` projection for this learning project so each query can map its result without a follow-up read.
This increases storage/write amplification and will be measured and discussed. GSI reads are eventually consistent.

## Item types

```text
DEPARTMENT#<id>                    METADATA
STUDENT#<id>                       METADATA
STUDENT#<id>                       PROFILE
INSTRUCTOR#<id>                    METADATA
COURSE#<id>                        METADATA
COURSE#<id>                        CAPACITY
ENROLLMENT#<id>                    METADATA             authoritative
STUDENT#<id>                       ENROLLMENT#<time>#<id> student relationship copy
COURSE#<id>                        ENROLLMENT#<time>#<id> course relationship copy
ENROLLMENT_PAIR#<student>#<course> ACTIVE               active duplicate claim
UNIQUE#<kind>#<normalized-value>   CLAIM                uniqueness owner
```

Metadata and authoritative enrollment items contain `entityType`, domain fields, ISO-8601 timestamps, and `version`.
Only items participating in an index contain that index's attributes, making all four GSIs sparse.

## Authority and denormalization

- Entity metadata is authoritative for each top-level resource.
- `ENROLLMENT#id/METADATA` is authoritative for enrollment state.
- Student and course enrollment copies duplicate IDs, status, enrollment timestamps, and small display summaries to
  answer relationship endpoints without unbounded application joins.
- The course `CAPACITY` item is authoritative for `occupiedSeats`; course metadata is authoritative for configured
  capacity and status. The transaction condition checks the capacity snapshot/version before incrementing.
- Unique and active-pair claims contain owner IDs and exist solely as integrity controls.

Enrollment copies, the active claim, and capacity changes are written in one transaction. Read repair and reconciliation
compare copies to the authoritative enrollment. Ordinary updates to names do not fan out synchronously to historic
enrollment summaries; API responses treat duplicated names as display snapshots and fetch current detail only for
bounded single-resource operations. This policy avoids unbounded write fan-out.

## Optimistic concurrency and referential integrity

Writes condition on the expected `version` and increment it explicitly. Creation conditionally requires absent keys.
Relationship creation uses transaction condition checks against referenced metadata. DynamoDB has no foreign keys:
the application owns these checks, and deletion/create races require transaction conditions on stable metadata/version
items. The adapter will translate conditional failures into domain-specific conflicts.

## Pagination and consistency

Queries expose `LastEvaluatedKey` as an authenticated opaque cursor. A cursor is bound to index, partition, sort
condition, and filter identity so it cannot be replayed against another query. Table reads may request strong
consistency when making a business decision. GSI queries are always eventually consistent; callers are not promised
read-your-write behavior on list endpoints.

## Capacity risk

The capacity item is a hot item for a very popular course. That is acceptable for this bounded learning domain and
provides simple correctness. A production system with extreme contention would evaluate sharded counters/reservations,
but those complicate exact capacity enforcement and are intentionally outside this checkpoint.

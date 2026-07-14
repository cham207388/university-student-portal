# DynamoDB table design

## Decision

Use six domain-oriented DynamoDB tables:

| Table | Partition key | Purpose |
| --- | --- | --- |
| `student-portal-departments` | `id` | Department records |
| `student-portal-students` | `id` | Student records and department/status access |
| `student-portal-student-profiles` | `studentId` | One profile per student |
| `student-portal-instructors` | `id` | Instructor records and department access |
| `student-portal-courses` | `id` | Course records, status, and authoritative occupied-seat count |
| `student-portal-enrollments` | `id` | Enrollment records plus active-pair lock items |

The design intentionally resembles the multi-table DynamoDB estate that motivated this learning project. Tables remain
NoSQL stores: IDs in other records are logical references, not foreign keys, and no ORM relationship or cascade exists.

## Indexes

All GSIs use `ALL` projection for the learning project. GSI reads are eventually consistent.

| Table | GSI | Keys | Access pattern |
| --- | --- | --- | --- |
| Departments | `departments-by-code` | `code` | Exact unique code lookup |
| Departments | `departments-catalog` | `entityType`, `createdAtId` | Bounded list of all departments |
| Students | `students-by-number` | `studentNumber` | Exact unique number lookup |
| Students | `students-by-email` | `email` | Exact normalized email lookup |
| Students | `students-by-department` | `departmentId`, `lastNameId` | Department students and supported last-name prefix |
| Students | `students-by-status` | `status`, `updatedAtId` | Status query |
| Students | `students-catalog` | `entityType`, `createdAtId` | Bounded list |
| Profiles | none | — | Direct student ID lookup only |
| Instructors | `instructors-by-number` | `employeeNumber` | Exact unique number lookup |
| Instructors | `instructors-by-email` | `email` | Exact normalized email lookup |
| Instructors | `instructors-by-department` | `departmentId`, `lastNameId` | Department instructors |
| Instructors | `instructors-catalog` | `entityType`, `createdAtId` | Bounded list |
| Courses | `courses-by-code` | `courseCode` | Exact unique code lookup |
| Courses | `courses-by-department` | `departmentId`, `courseCodeId` | Department courses |
| Courses | `courses-by-instructor` | `instructorId`, `courseCodeId` | Instructor courses |
| Courses | `courses-by-status` | `status`, `updatedAtId` | Status query |
| Courses | `courses-catalog` | `entityType`, `createdAtId` | Bounded list |
| Enrollments | `enrollments-by-student` | `studentId`, `enrolledAtId` | Student enrollments/date range |
| Enrollments | `enrollments-by-course` | `courseId`, `enrolledAtId` | Course enrollments/date range |
| Enrollments | `enrollments-by-status` | `status`, `enrolledAtId` | Status query |
| Enrollments | `enrollments-catalog` | `entityType`, `enrolledAtId` | Bounded list |

Catalog indexes avoid full-table scans for ordinary list endpoints. `entityType` is constant within a table but provides
the partition key required for an ordered, cursor-paginated `Query`.

## Records and authority

Each table has one authoritative record per logical entity. Records use UUID strings, ISO-8601 timestamps, and an
explicit numeric `version`. No enrollment relationship copies are required because the student/course GSIs index the
authoritative enrollment record.

Department, Student, Instructor, and Course tables also contain sparse `UNIQUE_CLAIM` records with deterministic
partition keys and an `ownerId`. They have no GSI key attributes and are therefore excluded from normal catalogs.

The enrollment table also contains short-lived/active integrity records with IDs shaped as
`ACTIVE#<studentId>#<courseId>`. A transaction conditionally creates this lock beside the authoritative enrollment and
deletes it when the enrollment becomes terminal. These records have `recordType=ACTIVE_ENROLLMENT_LOCK` and are absent
from catalog/status indexes, so migration readers ignore them.

The Course record owns `occupiedSeats` and an enrollment-history count; Student records also own an enrollment-history
count. Enrollment transactions conditionally update these counters and entity versions while creating or changing
enrollment state. This intentionally demonstrates a DynamoDB transaction spanning multiple tables.

## Integrity and consistency

- Primary-key creation uses a conditional put. GSIs cannot be read strongly and therefore cannot enforce alternate-key
  uniqueness; student numbers, emails, employee numbers, department/course codes use deterministic claim records in
  the same table, written transactionally with the authoritative record. Claim creation, replacement, and release are
  atomic with entity create, update, and delete respectively.
- Student, instructor, course, and enrollment services strongly read referenced records before writes and include
  transaction conditions where a create/delete race would violate integrity.
- Updates condition on `version` and increment it through the Enhanced Client version extension.
- Direct table reads can be strongly consistent. GSI queries are eventually consistent and do not promise immediate
  list visibility after writes.
- DynamoDB provides no foreign keys. Orphan detection and repair are explicit reconciliation operations.

## Pagination and scans

Every list query uses an index/table `LastEvaluatedKey`, encoded into an opaque cursor bound to table, index, and query
identity. Exact total counts, page offsets, arbitrary sorting, course-title substrings, global student last-name search,
credit ranges, and arbitrary filter combinations are unsupported in DynamoDB mode.

The cursor is a versioned URL-safe Base64 binary envelope containing the query identity and typed key attributes. It is
not a public bookmark format. Decoding rejects malformed cursors and cursors issued for another table, index, partition,
prefix, or date range. `limit` is bounded to 1–100, ordering follows the selected GSI sort key, and `hasNext` reflects the
presence of a DynamoDB `LastEvaluatedKey`; totals and offsets are intentionally absent.

Stored business timestamps remain ISO-8601. Derived timestamp GSI sort keys use a fixed-width signed-epoch/nanosecond
prefix followed by UUID, because variable-width ISO-8601 fractional seconds do not have reliable string sort order.

Migration and reconciliation are the deliberate scan exception. They scan each source table independently, paginate
all reads, select only authoritative `recordType` values, and track a checkpoint per table/entity type.

## Migration relevance

Six source tables force the migration to handle referential ordering, per-table checkpoints, different key/index
shapes, cross-table orphans, independent retry/failure boundaries, and source-to-target count reconciliation. Tests will
also introduce legacy aliases, invalid enums, missing references, duplicate logical identities, and JSON-encoded source
attributes to reflect risks found in the reference project.

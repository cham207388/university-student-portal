# DynamoDB access patterns

This inventory precedes and justifies the six-table design. Normal APIs use `GetItem`, `BatchGetItem`, or `Query`;
`Scan` is limited to explicit migration, reconciliation, and controlled administrative workflows. All collections are
bounded and cursor-paginated.

## Departments and students

| Operation | Input | Result/cardinality/order | Consistency | Key/index | Transaction |
| --- | --- | --- | --- | --- | --- |
| Get department | ID | One | Strong | Departments `id` | No |
| Get department by code | code | Zero/one | GSI eventual; deterministic claim for writes | `departments-by-code` | Create/update uniqueness claim |
| List departments | cursor, limit | Many by creation time/ID | Eventual | `departments-catalog` | No |
| Get student | ID | One | Strong | Students `id` | No |
| Get student by number/email | exact normalized value | Zero/one | GSI eventual; deterministic claims for writes | `students-by-number` / `students-by-email` | Create/update uniqueness claims |
| List students | cursor, limit | Many by creation time/ID | Eventual | `students-catalog` | No |
| List students by department | department ID, optional last-name prefix | Many by last name/ID | Eventual | `students-by-department` | No |
| List students by status | status | Many by update time/ID | Eventual | `students-by-status` | No |
| Create/update student | student, department, expected version | One | Strong checks | Students table plus Department condition | Yes when relationship/unique locks change |
| Get/put/delete profile | student ID, expected version | Zero/one | Strong | Profiles `studentId` | Student existence condition on put |

## Instructors and courses

| Operation | Input | Result/cardinality/order | Consistency | Key/index | Transaction |
| --- | --- | --- | --- | --- | --- |
| Get instructor | ID | One | Strong | Instructors `id` | No |
| Get instructor by employee number/email | exact normalized value | Zero/one | Eventual lookup; deterministic claims for writes | number/email indexes | Create/update uniqueness claims |
| List instructors | cursor, limit | Many by creation time/ID | Eventual | `instructors-catalog` | No |
| List instructors by department | department ID | Many by last name/ID | Eventual | `instructors-by-department` | No |
| Get course | ID | One including capacity state | Strong | Courses `id` | No |
| Get course by code | exact normalized code | Zero/one | Eventual lookup; deterministic claim for writes | `courses-by-code` | Create/update uniqueness claim |
| List courses | cursor, limit | Many by creation time/ID | Eventual | `courses-catalog` | No |
| List courses by department/instructor/status | relevant ID/status | Many in index order | Eventual | department/instructor/status index | No |
| Create/update course | course, department, instructor, version | One | Strong reference checks | Courses plus Department/Instructor conditions | Yes |
| Read/update occupied seats | course ID | One atomic counter | Strong | Course record | Enrollment transaction |

## Enrollments

| Operation | Input | Result/cardinality/order | Consistency | Key/index | Transaction |
| --- | --- | --- | --- | --- | --- |
| Get enrollment | ID | One authoritative record | Strong | Enrollments `id` | No |
| List enrollments | cursor, limit | Many by enrollment time/ID | Eventual | `enrollments-catalog` | No |
| List by student/course | ID, optional date range | Many by enrollment time/ID | Eventual | student/course index | No |
| List by status | status | Many by enrollment time/ID | Eventual | `enrollments-by-status` | No |
| Determine active duplicate | student ID, course ID | Zero/one lock | Strong | Enrollments `id=ACTIVE#student#course` | Conditional create/delete |
| Enroll without overbooking | student ID, course ID | Enrollment | Strong transaction semantics for touched records | Student, Course, Enrollment tables | Yes |
| Drop/complete enrollment | ID, version, target state | Updated enrollment/counter/lock | Strong | Enrollment and Course tables | Yes |

Enrollment creation transactionally checks student/course existence and course status/version, conditionally increments
`occupiedSeats`, creates the authoritative enrollment, and creates the deterministic active lock. Status changes update
the enrollment, counter when capacity consumption changes, and active lock atomically.

## Delete and integrity checks

| Operation | Input | Access | Limitation/guard |
| --- | --- | --- | --- |
| Delete department | department ID | limit-1 queries on student/instructor/course department indexes | Relationship writers condition on department existence/version |
| Delete student | student ID | profile get plus limit-1 enrollment query | Profile deleted in transaction; history blocks student deletion |
| Delete instructor | instructor ID | limit-1 instructor-course query | Course writer conditions on instructor existence/version |
| Delete course | course ID | limit-1 course-enrollment query | Enrollment writer conditions on course existence/version |
| Reconcile orphans | source table set | paginated scan of each authoritative record type | Explicit report; never silently delete |

## Explicitly unsupported DynamoDB queries

- Course-title substring search, global last-name substring search, credit ranges, arbitrary multi-filter combinations,
  arbitrary sorting, offsets, exact totals, and total pages.
- Unsupported requests return an RFC 9457 problem rather than scanning or sorting unbounded data in memory.
- PostgreSQL will support a broader validated filter/sort set, documented in the API compatibility matrix.

## Migration reads

The migration scans tables in referential order: Departments, Students, Profiles, Instructors, Courses, Enrollments.
Each table has its own `LastEvaluatedKey` checkpoint, counters, retry boundary, and rejected-record reporting. Enrollment
lock records are physical source items but not logical enrollment entities; reconciliation reports both counts.

## Implemented cursor capability ports

The common CRUD repositories remain persistence-neutral. Separate `Dynamo*Queries` capability ports expose only these
efficient query shapes:

- Department catalog.
- Student catalog, department with optional normalized last-name prefix, and status.
- Instructor catalog and department.
- Course catalog, department, instructor, and status.
- Enrollment catalog, student/course with optional inclusive date range, and status.

Each method requires a bounded `CursorRequest` and returns a domain-level `CursorPage`. The cursor identity includes the
physical table, GSI, partition value, and normalized prefix/range bounds, preventing accidental cursor reuse across
different filters. Exact alternate-key lookups remain separate common repository operations; unsupported filter
combinations never fall back to `Scan`.

# DynamoDB relationships

Relationships are UUID attributes, not foreign keys or Enhanced Client object mappings. `Student.departmentId`,
`Instructor.departmentId`, `Course.departmentId`, `Course.instructorId`, and Enrollment student/course IDs are logical
references whose targets live in separate tables.

Application services strongly read required targets before creating or changing a relationship:

- A Student or Instructor requires an existing Department.
- A Course requires an existing Department and Instructor.
- The Course instructor must belong to the Course department.
- A StudentProfile requires an existing Student and remains keyed by `studentId`.

Relationship collections use the documented GSIs and bounded cursor capability ports. DynamoDB performs no join;
services that need related representations query IDs in bounded pages and perform bounded batch reads.

Dependency-aware services reject Department, Student, Instructor, and Course deletes when bounded relationship probes
find dependents. Student deletion atomically removes its optional Profile. Student and Course enrollment-history counters
are updated by enrollment transactions, making their history-based delete rules authoritative under concurrency.

Strong pre-reads and Department/Instructor GSI probes do not lock another table. A new Student, Instructor, or Course can
still race a parent delete because GSIs are eventually consistent; parent dependency counters are required to eliminate
that final race. Reconciliation reports legacy or externally introduced orphans rather than silently deleting them.

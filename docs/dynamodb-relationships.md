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

Dependency-aware services reject Department, Student, Instructor, and Course deletes when dependents exist. Student
deletion atomically removes its optional Profile. Student and Course enrollment-history counters make their history-based
delete rules authoritative under concurrency.

Department and Instructor dependency guards are also authoritative. Department owns Student, Instructor, and Course
counters, while Instructor owns a Course counter. Every child create, relationship move, and delete transaction updates
the affected parent counters; parent deletion requires zero counters in its transaction. Competing relationship creation
and deletion therefore write the same parent item and cannot both commit. GSI probes remain eventual-consistency read
paths for friendly errors, not integrity authorities. Reconciliation reports legacy or externally introduced orphans
rather than silently deleting them.

Each first or repeated Enrollment also transactionally writes the deterministic durable edge
`RELATIONSHIP#<studentId>#<courseId>`. Two sparse edge GSIs support distinct Student-to-Courses and Course-to-Students
pages. Edge cursors are query-bound, and each at-most-100-ID page is hydrated from the target table with a strongly
consistent `BatchGetItem`; repeated enrollment history never duplicates the related entity.

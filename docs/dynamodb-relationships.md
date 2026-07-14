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

Strong pre-reads make validation deterministic at that instant but do not lock another table. A target could be deleted
between validation and the dependent write. Dependency-aware delete services and condition checks in the same DynamoDB
transaction are therefore still required before destructive operations are exposed. Reconciliation will report any
legacy or externally introduced orphan rather than silently deleting it.

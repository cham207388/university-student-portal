# PostgreSQL schema foundation

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

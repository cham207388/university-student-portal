# REST API contracts

## Conventions

- Base path: `/api/v1`
- JSON request and response DTOs only; persistence records are never exposed.
- UUID path identifiers.
- `POST` returns `201 Created` with a `Location` header.
- Successful reads and updates return `200 OK`; successful physical deletes return `204 No Content`.
- Validation and malformed input return `400`; missing resources return `404`; duplicates, invalid transitions, stale versions, and concurrency conflicts return `409`.
- Errors use RFC 9457 Problem Details. Validation problems add a `fieldErrors` array containing `field`, `message`, and rejected-value information only when safe.
- Update requests carry the last observed `version`; stale updates fail rather than silently overwriting changes.

## Resources

| Resource | Operations |
| --- | --- |
| Departments | `POST /departments`, `GET /departments/{id}`, `GET /departments`, `PUT /departments/{id}`, `DELETE /departments/{id}` |
| Department relationships | `GET /departments/{id}/students`, `/instructors`, `/courses` |
| Students | `POST /students`, `GET /students/{id}`, `GET /students`, `PUT /students/{id}`, `PATCH /students/{id}/status`, `DELETE /students/{id}` |
| Profiles | `PUT /students/{id}/profile`, `GET /students/{id}/profile`, `DELETE /students/{id}/profile` |
| Student relationships | `GET /students/{id}/enrollments`, `/courses` |
| Instructors | `POST /instructors`, `GET /instructors/{id}`, `GET /instructors`, `PUT /instructors/{id}`, `DELETE /instructors/{id}`, `GET /instructors/{id}/courses` |
| Courses | `POST /courses`, `GET /courses/{id}`, `GET /courses`, `PUT /courses/{id}`, `PATCH /courses/{id}/status`, `DELETE /courses/{id}` |
| Course relationships | `GET /courses/{id}/students`, `/enrollments` |
| Enrollments | `POST /enrollments`, `GET /enrollments/{id}`, `GET /enrollments`, `PATCH /enrollments/{id}/status`, `DELETE /enrollments/{id}` |

All paths in the table are relative to `/api/v1`.

## Request boundaries

Create and replace DTOs contain writable business fields, not IDs or audit timestamps. Status changes use a dedicated request containing the target status and expected version. Enrollment creation requires `studentId` and `courseId`; its initial status defaults to `ENROLLED`. Profile `PUT` is idempotent create-or-replace for the student's single profile.

Response DTOs contain the resource ID, business fields, audit timestamps, and version. Relationship endpoints return bounded summaries rather than recursively nested aggregates.

## Filtering

- Departments in DynamoDB mode: unfiltered catalog or exact `code`.
- Students in DynamoDB mode: unfiltered catalog, `departmentId` with optional `lastName` prefix, `status`, exact
  `studentNumber`, or exact `email`.
- Instructors in DynamoDB mode: unfiltered catalog, `departmentId`, exact `employeeNumber`, or exact `email`.
- Courses in DynamoDB mode: unfiltered catalog or exactly one of `departmentId`, `instructorId`, `status`, and exact
  `courseCode`.
- Enrollments in DynamoDB mode: unfiltered catalog or exactly one of `studentId`, `courseId`, and `status`.
  `enrolledFrom` and `enrolledTo` are supported only with `studentId` or `courseId`.

Exact alternate-key filters return the normal cursor-page envelope with zero or one item, `limit: 1`, and no cursor.
They cannot be combined with another filter or a cursor. Alternate-key GSIs are eventually consistent read paths;
transactional claim records remain the uniqueness authority.

Every `/api/v1/**` handler rejects undeclared query parameters before controller execution. Unsupported filter
combinations, unknown filters, and sorting return a clear `400` Problem Detail. The application will not hide a
DynamoDB scan or unbounded in-memory sort behind these contracts.

## Pagination variants

DynamoDB collections use `limit` and an opaque `cursor`, returning `content`, `limit`, `nextCursor`, and `hasNext`. PostgreSQL collections use zero-based `page`, bounded `size`, and validated `sort`, returning content plus totals and navigation metadata.

A DynamoDB cursor is valid only for the table, index, partition, prefix, and date-range query that issued it. Reusing it
with different filters, or supplying malformed cursor data, returns a `400` invalid-request Problem Detail.

Because offsets, arbitrary sorting, and exact totals are not efficient DynamoDB access patterns, these variants are explicit database capabilities. Contract parity tests will verify shared resource semantics and separately verify the pagination differences.

## DynamoDB controller implementation

The DynamoDB profile exposes all resource create/get/update/status/delete operations, Student Profile operations,
Enrollment operations, catalogs, Department child collections, Instructor Courses, Student/Course Enrollment collections,
and the derived Student/Course many-to-many views. Physical deletes carry the expected version as a required `version`
query parameter.

Derived views page deterministic relationship-edge GSIs and hydrate each bounded page with strongly consistent
`BatchGetItem` calls. Re-enrollment does not duplicate a Student or Course in these views. Exact alternate-key reads by
email, student/employee number, or department/course code are exposed as the collection filters described above. Title
search and credit ranges are not exposed in DynamoDB mode and are rejected explicitly. No controller substitutes a scan.

## Delete semantics

Department, student, profile, instructor, and course deletion is physical only when the domain deletion policy permits it. Deleting an enrollment performs a domain-level transition to `DROPPED` and returns the updated enrollment representation, because history is retained.

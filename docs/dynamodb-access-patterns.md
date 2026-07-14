# DynamoDB access patterns

This inventory precedes the key design. Every normal API operation must use `GetItem`, `BatchGetItem`, or `Query`.
`Scan` is reserved for explicit migration/reconciliation iteration and controlled administrative seeding checks.

Abbreviations used in the candidate keys: `PK`/`SK` are table keys and `G1`–`G4` are sparse global secondary indexes.
All list operations are bounded and use `LastEvaluatedKey` cursor pagination.

## Entity and relationship patterns

| Operation | Inputs | Result/cardinality/order | Consistency | Candidate keys/index | Transaction |
| --- | --- | --- | --- | --- | --- |
| Get department | department ID | One | Strong | `PK=DEPARTMENT#id`, `SK=METADATA` | No |
| Get department by code | normalized code | One | Strong claim then entity read | `PK=UNIQUE#DEPARTMENT_CODE#code`, `SK=CLAIM`; then metadata | No |
| List departments | cursor, limit | Many, creation time then ID | Eventual | `G1PK=ENTITY#DEPARTMENT`, `G1SK=createdAt#id` | No |
| Get student | student ID | One | Strong | `PK=STUDENT#id`, `SK=METADATA` | No |
| Get student by number | normalized number | One | Strong claim then entity read | unique claim item, then metadata | No |
| Get student by email | normalized email | One | Strong claim then entity read | unique claim item, then metadata | No |
| List students | cursor, limit | Many, creation time then ID | Eventual | `G1PK=ENTITY#STUDENT` | No |
| List students by department | department ID | Many, last name then ID | Eventual | `G2PK=DEPARTMENT#id`, `G2SK=STUDENT#lastName#id` | No |
| List students by status | status | Many, update time then ID | Eventual | `G3PK=STATUS#STUDENT#status` | No |
| Get student profile | student ID | Zero or one | Strong | `PK=STUDENT#id`, `SK=PROFILE` | No |
| Put student profile | student ID, version | One | Strong conditional write | profile key; condition on version/nonexistence | No |
| Get instructor | instructor ID | One | Strong | `PK=INSTRUCTOR#id`, `SK=METADATA` | No |
| Get instructor by number/email | normalized value | One | Strong claim then entity read | unique claim item, then metadata | No |
| List instructors | cursor, limit | Many, creation time then ID | Eventual | `G1PK=ENTITY#INSTRUCTOR` | No |
| List instructors by department | department ID | Many, last name then ID | Eventual | `G2PK=DEPARTMENT#id`, `G2SK=INSTRUCTOR#lastName#id` | No |
| Get course | course ID | One | Strong | `PK=COURSE#id`, `SK=METADATA` | No |
| Get course by code | normalized code | One | Strong claim then entity read | unique claim item, then metadata | No |
| List courses | cursor, limit | Many, creation time then ID | Eventual | `G1PK=ENTITY#COURSE` | No |
| List courses by department | department ID | Many, course code then ID | Eventual | `G2PK=DEPARTMENT#id`, `G2SK=COURSE#code#id` | No |
| List courses by instructor | instructor ID | Many, course code then ID | Eventual | `G4PK=INSTRUCTOR#id`, `G4SK=COURSE#code#id` | No |
| List courses by status | status | Many, update time then ID | Eventual | `G3PK=STATUS#COURSE#status` | No |
| Read course capacity state | course ID | One | Strong | `PK=COURSE#id`, `SK=CAPACITY` | No |

## Enrollment patterns

| Operation | Inputs | Result/cardinality/order | Consistency | Candidate keys/index | Transaction |
| --- | --- | --- | --- | --- | --- |
| Get enrollment | enrollment ID | One authoritative item | Strong | `PK=ENROLLMENT#id`, `SK=METADATA` | No |
| List enrollments | cursor, limit | Many logical items, enrollment time then ID | Eventual | authoritative item on `G1PK=ENTITY#ENROLLMENT` | No |
| List enrollments by student | student ID, optional date range | Many, enrollment time then ID | Strong when business decision needs it; otherwise eventual | `PK=STUDENT#id`, `SK begins_with ENROLLMENT#` | No |
| List courses for student | student ID | Many bounded enrollment summaries | Same as preceding | student enrollment copies contain bounded course summary/ID | No |
| List enrollments by course | course ID, optional date range | Many, enrollment time then ID | Strong when business decision needs it; otherwise eventual | `PK=COURSE#id`, `SK begins_with ENROLLMENT#` | No |
| List students for course | course ID | Many bounded enrollment summaries | Same as preceding | course enrollment copies contain bounded student summary/ID | No |
| List enrollments by status | status | Many, enrollment time then ID | Eventual | authoritative item on `G3PK=STATUS#ENROLLMENT#status` | No |
| Detect active duplicate | student ID, course ID | Zero or one | Strong | `PK=ENROLLMENT_PAIR#studentId#courseId`, `SK=ACTIVE` | Condition in enrollment transaction |
| Count capacity-consuming enrollments | course ID | One counter, not an unbounded count query | Strong | course capacity item | Updated in enrollment transaction |
| Enroll without overbooking | student ID, course ID | Enrollment plus copies | Serializable transaction semantics for touched items | student/course existence checks, active claim, capacity condition, authoritative and relationship items | Yes |
| Drop enrollment | enrollment ID/version | Updated authoritative item and both copies; delete active claim; decrement counter if needed | Strong transaction | enrollment keys and course capacity | Yes |
| Complete enrollment | enrollment ID/version, grade | Updated authoritative item and both copies; delete active claim | Strong transaction | enrollment keys | Yes |

An enrollment write uses no more than ten transaction actions: condition checks for student/course, conditional capacity
update, active-pair claim, authoritative enrollment, student copy, and course copy. Updates/deletes operate on the same
owned set. The implementation must calculate the exact action count and stay within DynamoDB transaction limits.

## Integrity and deletion patterns

| Operation | Inputs | Result | Candidate access | Transaction |
| --- | --- | --- | --- | --- |
| Claim unique department/course code | normalized code, owner ID | One claim | conditional put at unique key | Same transaction as metadata |
| Claim unique student/instructor number and email | normalized values, owner ID | Two claims | conditional puts at unique keys | Same transaction as metadata |
| Verify department exists | department ID | Boolean | strongly consistent metadata get/transaction condition | On relationship create/update |
| Verify instructor exists and belongs to department | IDs | Instructor metadata | strong read followed by transaction condition on version/department snapshot | On course create/update |
| Determine student has enrollment history | student ID | Boolean | query student partition with enrollment prefix, limit 1 | Before delete |
| Determine course has enrollments | course ID | Boolean | query course partition with enrollment prefix, limit 1 | Before delete |
| Determine department has dependents | department ID | Boolean | `G2` queries for each type, limit 1 | Before delete; documented race guarded by relationship write conditions |
| Determine instructor has courses | instructor ID | Boolean | `G4` query, limit 1 | Before delete; documented race guarded by course write conditions |

## Supported filters and explicit rejections

- Exact student email/number and course code use unique claims.
- Department, instructor, status, and enrollment date access uses keys above.
- Student last-name prefix is supported only with `departmentId`, where it is part of `G2SK`.
- Course title substring, global last-name substring, arbitrary multi-filter combinations, and credit ranges are not
  efficient in this design and return an unsupported-filter problem in DynamoDB mode.
- DynamoDB sorting is fixed by the selected key. Arbitrary sort fields and directions are rejected.
- No endpoint calculates total pages or exact totals. A cursor encodes only a validated `LastEvaluatedKey` plus query
  identity; it is opaque and tamper-resistant.

Migration source iteration is the deliberate exception to the no-scan rule. It scans only authoritative items,
paginates every request, and deduplicates relationship copies deterministically.

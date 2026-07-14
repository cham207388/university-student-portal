# DynamoDB transactions

## Alternate-key uniqueness

DynamoDB GSIs are eventually consistent and cannot enforce uniqueness. Each unique business value owns a deterministic,
sparse claim item in the authoritative entity table:

| Entity | Claim IDs |
| --- | --- |
| Department | `UNIQUE#CODE#<normalized-code>` |
| Student | `UNIQUE#STUDENT_NUMBER#<number>`, `UNIQUE#EMAIL#<normalized-email>` |
| Instructor | `UNIQUE#EMPLOYEE_NUMBER#<number>`, `UNIQUE#EMAIL#<normalized-email>` |
| Course | `UNIQUE#COURSE_CODE#<normalized-code>` |

Claims contain the table partition key, `recordType=UNIQUE_CLAIM`, and `ownerId`. They omit every GSI attribute, so
catalog and relationship queries return only authoritative records.

- Create transactionally condition-puts the authoritative record and every claim.
- Update condition-puts the new record at the expected version, deletes changed old claims only when owned by that
  entity, and condition-puts new claims.
- Delete checks the entity version and deletes all owned claims in one transaction.

Any failed condition cancels the whole transaction and becomes a `409` conflict. Thus a failed multi-claim Student or
Instructor write leaves neither a partial entity nor a partial claim, and concurrent writers for one value have one
winner. GSI exact lookups remain useful reads but are never the uniqueness authority.

## Enrollment and capacity

Enrollment creation is one four-action transaction:

1. Update the Student only when it exists and is `ACTIVE`, incrementing its enrollment-history count and version.
2. Update the Course only when it exists and is `OPEN`; increment its history count/version and, for an enrolled seat,
   increment `occupiedSeats` only when it remains below `capacity`.
3. Condition-put the authoritative Enrollment.
4. Condition-put `ACTIVE#<studentId>#<courseId>` with the Enrollment as owner.

The deterministic lock makes concurrent duplicate active enrollment attempts single-winner. The conditional Course
update makes concurrent last-seat attempts single-winner. A failure cancels Student/Course counters, Enrollment, and lock.

Status transitions condition-put the Enrollment at its expected version. `WAITLISTED → ENROLLED` conditionally consumes
capacity; a capacity-consuming transition to `DROPPED` decrements it; `ENROLLED → COMPLETED` retains the historical seat
count according to the domain rule. Leaving an active state deletes the owned lock in the same transaction. Physical
Enrollment deletion is not exposed—the application DELETE use case executes the transactional drop transition.

## Dependency-aware deletion

Student and Course records retain authoritative enrollment-history counters. Enrollment creation increments those
counters and entity versions transactionally. Student/Course deletion rejects nonzero counters; a concurrent enrollment
also changes the expected version, closing the gap even if a relationship GSI is briefly stale. Student deletion includes
its optional Profile and uniqueness claims in one transaction.

Department records own authoritative `studentCount`, `instructorCount`, and `courseCount` values; Instructor records own
an authoritative `courseCount`. Creating, moving, or deleting a child updates the affected parent counters and versions
in the same transaction as the child write. Department and Instructor deletion conditionally requires every owned
counter to be zero. Because both operations write the same parent item, a concurrent child mutation and parent delete
cannot both commit: one transaction wins and the other returns a conflict. Relationship GSIs remain useful for lists and
friendly preflight errors, but they are not the deletion authority.

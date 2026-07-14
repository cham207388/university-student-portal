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

## Planned enrollment transaction

Enrollment/capacity consistency remains the next transactional checkpoint. It will atomically condition-check Student
and Course state, update Course occupied seats, write Enrollment state, and maintain the deterministic active-pair lock.
Dependency-aware deletes will similarly combine relationship checks with destructive writes to close cross-table races.

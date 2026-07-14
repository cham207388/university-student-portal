# DynamoDB development data and live API testing

The DynamoDB development seeder is opt-in. It is active only when the `local-dynamodb` profile and
`student-portal.seed.enabled=true` are both present. The environment variable
`STUDENT_PORTAL_SEED_ENABLED=true` supplies that property in the standard local configuration. Normal application and
test startup do not seed data.

Records use name-based deterministic UUIDs. Each entity is strongly read before creation, so reruns preserve existing
records and can resume a partially completed seed. All writes go through the normal repositories: uniqueness claims,
Department/Instructor dependency counters, Course capacity, Student/Course enrollment counts, and active-enrollment
locks therefore follow the same transactions as API writes.

The fictional dataset contains:

- 3 Departments.
- 10 Students and 10 one-to-one Profiles.
- 5 Instructors.
- 10 Courses across `DRAFT`, `OPEN`, `CLOSED`, `CANCELLED`, and `COMPLETED` states.
- 6 Enrollments across `ENROLLED`, `WAITLISTED`, `DROPPED`, and `COMPLETED` states.
- A two-seat Course with both capacity-consuming seats occupied.
- Students in `ACTIVE`, `INACTIVE`, and `GRADUATED` states.

## Workflow

```shell
export LOCALSTACK_AUTH_TOKEN="<your-token>"
make localstack-up
make terraform-init
make terraform-apply
make app-run-dynamodb-seeded
```

Keep the application running and execute this in another terminal:

```shell
make api-smoke
```

The smoke script checks health, strongly consistent resource/profile reads, direct and derived relationship collections,
pagination, Enrollment reads, validation errors, unsupported-filter errors, and not-found Problem Details. Set
`STUDENT_PORTAL_API_URL` to test a non-default base URL.

The script uses these stable seed IDs:

| Record | ID |
| --- | --- |
| Computing Department | `b6206ea3-c883-3635-8eda-bac4f678ff66` |
| First Student | `19bc8b48-9f9d-3ca2-b63c-4ffbee93e9cd` |
| First Instructor | `5bb6835f-93e2-3c94-975d-3e2d9dd942a9` |
| Full Course | `edc7779c-00ae-38a1-b016-9f3b2971a722` |
| First Enrollment | `2abc2136-f9fd-397f-be90-22c7d9422fa7` |

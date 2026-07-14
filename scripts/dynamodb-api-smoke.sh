#!/usr/bin/env bash
set -euo pipefail

base_url="${STUDENT_PORTAL_API_URL:-http://127.0.0.1:8080}"
department_id="b6206ea3-c883-3635-8eda-bac4f678ff66"
student_id="19bc8b48-9f9d-3ca2-b63c-4ffbee93e9cd"
instructor_id="5bb6835f-93e2-3c94-975d-3e2d9dd942a9"
course_id="edc7779c-00ae-38a1-b016-9f3b2971a722"
enrollment_id="2abc2136-f9fd-397f-be90-22c7d9422fa7"
body_file="${TMPDIR:-/tmp}/student-portal-smoke-body.$$"
trap 'rm -f "$body_file"' EXIT

request() {
  expected="$1"
  method="$2"
  path="$3"
  data="${4:-}"
  args=(-sS -o "$body_file" -w '%{http_code}' -X "$method")
  if [[ -n "$data" ]]; then
    args+=(-H 'Content-Type: application/json' --data "$data")
  fi
  actual="$(curl "${args[@]}" "$base_url$path")"
  if [[ "$actual" != "$expected" ]]; then
    printf 'Expected HTTP %s but received %s for %s %s\n' "$expected" "$actual" "$method" "$path" >&2
    sed -n '1,20p' "$body_file" >&2
    exit 1
  fi
  printf 'ok %s %s -> %s\n' "$method" "$path" "$actual"
}

request 200 GET /actuator/health
request 200 GET "/api/v1/departments/$department_id"
request 200 GET "/api/v1/students/$student_id/profile"
request 200 GET "/api/v1/instructors/$instructor_id/courses?limit=5"
request 200 GET "/api/v1/courses/$course_id/enrollments?limit=5"
request 200 GET "/api/v1/students/$student_id/courses?limit=5"
request 200 GET "/api/v1/courses/$course_id/students?limit=5"
request 200 GET "/api/v1/enrollments/$enrollment_id"
request 200 GET '/api/v1/departments?limit=2'
request 400 GET '/api/v1/courses?status=OPEN&departmentId=b6206ea3-c883-3635-8eda-bac4f678ff66'
request 400 POST /api/v1/students '{}'
request 404 GET /api/v1/courses/00000000-0000-0000-0000-000000000000

printf 'DynamoDB API smoke workflow passed.\n'

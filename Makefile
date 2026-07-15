.PHONY: compose-up compose-down dynamodb-health postgres-health postgres-secret \
	tf-init tf-validate tf-plan tf-apply tf-destroy \
	app-run-dynamodb seed-dynamo-data app-run-dynamodb-seeded app-run-postgres migrate-dynamo-to-postgres \
	api-smoke api-smoke-dynamodb api-smoke-postgres check

compose-up:
	docker compose up -d

compose-down:
	docker compose down -v --remove-orphans

postgres-health:
	@aws --endpoint-url="$${AWS_ENDPOINT_URL:-http://127.0.0.1:4566}" rds describe-db-instances \
		--db-instance-identifier "$${POSTGRES_INSTANCE_ID:-student-portal-postgres}" \
		--query 'DBInstances[0].DBInstanceStatus' --output text

dynamodb-health:
	@set -e; \
	for table in \
		"$${DYNAMODB_DEPARTMENTS_TABLE:-student-portal-departments}" \
		"$${DYNAMODB_STUDENTS_TABLE:-student-portal-students}" \
		"$${DYNAMODB_STUDENT_PROFILES_TABLE:-student-portal-student-profiles}" \
		"$${DYNAMODB_INSTRUCTORS_TABLE:-student-portal-instructors}" \
		"$${DYNAMODB_COURSES_TABLE:-student-portal-courses}" \
		"$${DYNAMODB_ENROLLMENTS_TABLE:-student-portal-enrollments}"; do \
		status="$$(aws --endpoint-url="$${AWS_ENDPOINT_URL:-http://127.0.0.1:4566}" dynamodb describe-table \
			--table-name "$$table" --query 'Table.TableStatus' --output text)"; \
		printf '%s: %s\n' "$$table" "$$status"; \
		test "$$status" = "ACTIVE"; \
	done

postgres-secret:
	@aws --endpoint-url="$${AWS_ENDPOINT_URL:-http://127.0.0.1:4566}" secretsmanager get-secret-value \
		--secret-id "$${POSTGRES_SECRET_ID:-student-portal-postgres/credentials}" \
		--query SecretString --output text

tf-init:
	terraform -chdir=infrastructure/local init -upgrade

tf-validate:
	terraform -chdir=infrastructure/local validate

tf-plan:
	terraform -chdir=infrastructure/local plan

tf-apply: tf-init
	terraform -chdir=infrastructure/local apply -auto-approve

tf-destroy: tf-init
	terraform -chdir=infrastructure/local destroy -auto-approve

app-run-dynamodb:
	cd student-portal-api && ./gradlew bootRun --args='--spring.profiles.active=local-dynamodb --server.port=8081'

seed-dynamo-data:
	cd student-portal-api && STUDENT_PORTAL_SEED_ENABLED=true ./gradlew bootRun --args='--spring.profiles.active=local-dynamodb --spring.main.web-application-type=none --student-portal.seed.exit=true'

app-run-dynamodb-seeded:
	cd student-portal-api && STUDENT_PORTAL_SEED_ENABLED=true ./gradlew bootRun --args='--spring.profiles.active=local-dynamodb --server.port=8081'

app-run-postgres:
	cd student-portal-api && ./gradlew bootRun --args='--spring.profiles.active=local-postgres --server.port=8082'

migrate-dynamo-to-postgres:
	cd student-portal-api && ./gradlew bootRun --args='--spring.profiles.active=migration --spring.main.web-application-type=none --migration.run=true'

api-smoke:
	./scripts/api-smoke.sh

api-smoke-dynamodb:
	STUDENT_PORTAL_API_URL=http://127.0.0.1:8081 ./scripts/api-smoke.sh

api-smoke-postgres:
	STUDENT_PORTAL_API_URL=http://127.0.0.1:8082 ./scripts/api-smoke.sh

check:
	cd student-portal-api && ./gradlew clean check

.PHONY: compose-up compose-down postgres-up postgres-health postgres-secret \
	tf-init tf-validate tf-plan tf-apply tf-destroy \
	app-run-dynamodb seed-dynamo-data app-run-dynamodb-seeded app-run-postgres migrate-dynamo-to-postgres api-smoke check

compose-up:
	docker compose up -d --remove-orphans

compose-down:
	docker compose down -v

postgres-up: compose-up tf-apply
	@echo "LocalStack RDS PostgreSQL provisioned; wait for availability with 'make postgres-health'."

postgres-health:
	@aws --endpoint-url="$${AWS_ENDPOINT_URL:-http://127.0.0.1:4566}" rds describe-db-instances \
		--db-instance-identifier "$${POSTGRES_INSTANCE_ID:-student-portal-postgres}" \
		--query 'DBInstances[0].DBInstanceStatus' --output text

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
	cd student-portal-api && ./gradlew bootRun --args='--spring.profiles.active=local-dynamodb'

seed-dynamo-data:
	cd student-portal-api && STUDENT_PORTAL_SEED_ENABLED=true ./gradlew bootRun --args='--spring.profiles.active=local-dynamodb --student-portal.seed.exit=true'

app-run-dynamodb-seeded:
	cd student-portal-api && STUDENT_PORTAL_SEED_ENABLED=true ./gradlew bootRun --args='--spring.profiles.active=local-dynamodb'

app-run-postgres:
	cd student-portal-api && ./gradlew bootRun --args='--spring.profiles.active=local-postgres'

migrate-dynamo-to-postgres:
	cd student-portal-api && ./gradlew bootRun --args='--spring.profiles.active=migration --spring.main.web-application-type=none'


api-smoke:
	./scripts/dynamodb-api-smoke.sh

check:
	cd student-portal-api && ./gradlew clean check

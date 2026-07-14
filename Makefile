.PHONY: localstack-up localstack-down postgres-up postgres-down postgres-health \
	terraform-init terraform-validate terraform-plan terraform-apply terraform-destroy \
	app-run-dynamodb app-run-dynamodb-seeded api-smoke check

localstack-up:
	docker compose up -d --remove-orphans localstack

localstack-down:
	docker compose down

postgres-up:
	docker compose up -d --remove-orphans localstack
	terraform -chdir=infrastructure/local apply -auto-approve

postgres-down:
	terraform -chdir=infrastructure/local destroy -auto-approve

postgres-health:
	@aws --endpoint-url="$${AWS_ENDPOINT_URL:-http://127.0.0.1:4566}" rds describe-db-instances \
		--db-instance-identifier "$${POSTGRES_INSTANCE_ID:-student-portal-postgres}" \
		--query 'DBInstances[0].DBInstanceStatus' --output text

terraform-init:
	terraform -chdir=infrastructure/local init

terraform-validate:
	terraform -chdir=infrastructure/local validate

terraform-plan:
	terraform -chdir=infrastructure/local plan

terraform-apply:
	terraform -chdir=infrastructure/local apply

terraform-destroy:
	terraform -chdir=infrastructure/local destroy

app-run-dynamodb:
	cd student-portal-api && ./gradlew bootRun --args='--spring.profiles.active=local-dynamodb'

app-run-dynamodb-seeded:
	cd student-portal-api && STUDENT_PORTAL_SEED_ENABLED=true ./gradlew bootRun --args='--spring.profiles.active=local-dynamodb'

api-smoke:
	./scripts/dynamodb-api-smoke.sh

check:
	cd student-portal-api && ./gradlew clean check

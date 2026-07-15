.PHONY: compose-up compose-down postgres-health \
	tf-init tf-validate tf-plan tf-apply tf-destroy \
	app-run-dynamodb app-run-dynamodb-seeded api-smoke check

compose-up:
	docker compose up -d --remove-orphans localstack

compose-down:
	docker compose down -v

postgres-health:
	@aws --endpoint-url="$${AWS_ENDPOINT_URL:-http://127.0.0.1:4566}" rds describe-db-instances \
		--db-instance-identifier "$${POSTGRES_INSTANCE_ID:-student-portal-postgres}" \
		--query 'DBInstances[0].DBInstanceStatus' --output text

tf-init:
	terraform -chdir=infrastructure/local init

tf-validate:
	terraform -chdir=infrastructure/local validate

tf-plan:
	terraform -chdir=infrastructure/local plan

tf-apply:
	terraform -chdir=infrastructure/local apply -auto-approve

tf-destroy:
	terraform -chdir=infrastructure/local destroy -auto-approve

app-run-dynamodb:
	cd student-portal-api && ./gradlew bootRun --args='--spring.profiles.active=local-dynamodb'

app-run-dynamodb-seeded:
	cd student-portal-api && STUDENT_PORTAL_SEED_ENABLED=true ./gradlew bootRun --args='--spring.profiles.active=local-dynamodb'

api-smoke:
	./scripts/dynamodb-api-smoke.sh

check:
	cd student-portal-api && ./gradlew clean check

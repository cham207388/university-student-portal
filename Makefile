.PHONY: localstack-up localstack-down postgres-up postgres-down postgres-health \
	terraform-init terraform-validate terraform-plan terraform-apply terraform-destroy \
	app-run-dynamodb app-run-dynamodb-seeded api-smoke check

localstack-up:
	docker compose up -d localstack

localstack-down:
	docker compose down

postgres-up:
	docker compose up -d postgres

postgres-down:
	docker compose stop postgres

postgres-health:
	docker compose ps postgres
	@docker compose exec -T postgres pg_isready -U "$${POSTGRES_USER:-student_portal}" -d "$${POSTGRES_DB:-student_portal}"

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

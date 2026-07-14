.PHONY: localstack-up localstack-down terraform-init terraform-validate terraform-plan terraform-apply terraform-destroy check

localstack-up:
	docker compose up -d localstack

localstack-down:
	docker compose down

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

check:
	cd student-portal-api && ./gradlew clean check

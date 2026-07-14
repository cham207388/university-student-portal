# Student Portal API developer help

Run commands from the repository root unless noted otherwise.

## Verify

```shell
make check
```

The Gradle `check` task includes unit/MVC tests and the isolated LocalStack Testcontainers suite.

## Run with DynamoDB

Provision the six tables before starting the API:

```shell
export LOCALSTACK_AUTH_TOKEN="<your-token>"
make localstack-up
make terraform-init
make terraform-validate
make terraform-apply
make app-run-dynamodb-seeded
```

Keep the last command running. In another terminal:

```shell
make api-smoke
```

The application is available at `http://127.0.0.1:8080`; health is at `/actuator/health` and resources are under
`/api/v1`. Import the repository's `postman.json` for interactive testing.

See the root `README.md` and `docs/dynamodb-development-data.md` for configuration, seed contents, stable IDs, and the
documented LocalStack Terraform GSI caveat.

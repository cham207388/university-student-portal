provider "aws" {
  region     = var.aws_region
  access_key = "test"
  secret_key = "test"

  endpoints {
    dynamodb       = var.aws_endpoint
    rds            = var.aws_endpoint
    secretsmanager = var.aws_endpoint
  }

  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true
}

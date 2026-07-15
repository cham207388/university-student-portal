variable "aws_region" {
  description = "AWS region emulated by LocalStack."
  type        = string
  default     = "us-east-1"
}

variable "aws_endpoint" {
  description = "LocalStack gateway used for DynamoDB."
  type        = string
  default     = "http://localhost:4566"
}

variable "table_prefix" {
  description = "Prefix shared by Student Portal DynamoDB tables."
  type        = string
  default     = "student-portal"
}

variable "postgres_identifier" {
  description = "Identifier for the LocalStack RDS PostgreSQL instance."
  type        = string
  default     = "student-portal-postgres"
}

variable "postgres_database" {
  description = "Initial PostgreSQL database name."
  type        = string
  default     = "student_portal"
}

variable "postgres_username" {
  description = "PostgreSQL master username."
  type        = string
  default     = "student_portal"
}

variable "postgres_password" {
  description = "PostgreSQL master password for local development."
  type        = string
  sensitive   = true
  default     = "student_portal_local"
}

variable "postgres_host" {
  description = "Host name exposed by LocalStack for the emulated PostgreSQL instance."
  type        = string
  default     = "localhost.localstack.cloud"
}

variable "postgres_external_port" {
  description = "Host port allocated by LocalStack for the emulated PostgreSQL instance."
  type        = number
  default     = 4510
}

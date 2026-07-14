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

variable "table_name" {
  description = "Student Portal DynamoDB table name."
  type        = string
  default     = "student-portal"
}

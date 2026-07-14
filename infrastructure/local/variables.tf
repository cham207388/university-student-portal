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

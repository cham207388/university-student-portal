output "dynamodb_table_name" {
  description = "Table name consumed by local application configuration."
  value       = module.student_portal_dynamodb.table_name
}

output "dynamodb_table_arn" {
  description = "LocalStack DynamoDB table ARN."
  value       = module.student_portal_dynamodb.table_arn
}

output "dynamodb_endpoint" {
  description = "DynamoDB endpoint consumed by local application configuration."
  value       = var.aws_endpoint
}

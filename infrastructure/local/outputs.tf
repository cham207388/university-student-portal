output "dynamodb_table_names" {
  description = "Table names consumed by local application configuration."
  value       = { for name, table in module.student_portal_dynamodb : name => table.table_name }
}

output "dynamodb_table_arns" {
  description = "LocalStack DynamoDB table ARNs."
  value       = { for name, table in module.student_portal_dynamodb : name => table.table_arn }
}

output "dynamodb_endpoint" {
  description = "DynamoDB endpoint consumed by local application configuration."
  value       = var.aws_endpoint
}

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

output "postgres_endpoint" {
  description = "LocalStack RDS PostgreSQL endpoint."
  value       = aws_db_instance.postgres.address
}

output "postgres_port" {
  value = aws_db_instance.postgres.port
}

output "postgres_database" {
  value = var.postgres_database
}

output "postgres_secret_arn" {
  description = "Secret containing LocalStack RDS PostgreSQL credentials."
  value       = aws_secretsmanager_secret.postgres.arn
}

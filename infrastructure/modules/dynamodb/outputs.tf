output "table_name" {
  description = "Provisioned DynamoDB table name."
  value       = aws_dynamodb_table.this.name
}

output "table_arn" {
  description = "Provisioned DynamoDB table ARN."
  value       = aws_dynamodb_table.this.arn
}

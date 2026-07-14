module "student_portal_dynamodb" {
  source = "../modules/dynamodb"

  table_name = var.table_name
  tags       = local.common_tags
}

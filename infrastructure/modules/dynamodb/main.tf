locals {
  table_keys = merge(
    { (var.partition_key.name) = var.partition_key.type },
    var.sort_key == null ? {} : { (var.sort_key.name) = var.sort_key.type }
  )

  index_keys = merge([
    for index in var.global_secondary_indexes : merge(
      { (index.partition_key.name) = index.partition_key.type },
      try(index.sort_key, null) == null ? {} : { (index.sort_key.name) = index.sort_key.type }
    )
  ]...)

  key_attributes = merge(local.table_keys, local.index_keys)
}

resource "aws_dynamodb_table" "this" {
  name         = var.table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = var.partition_key.name
  range_key    = var.sort_key == null ? null : var.sort_key.name

  dynamic "attribute" {
    for_each = local.key_attributes
    content {
      name = attribute.key
      type = attribute.value
    }
  }

  dynamic "global_secondary_index" {
    for_each = { for index in var.global_secondary_indexes : index.name => index }
    content {
      name            = global_secondary_index.value.name
      projection_type = "ALL"

      key_schema {
        attribute_name = global_secondary_index.value.partition_key.name
        key_type       = "HASH"
      }

      dynamic "key_schema" {
        for_each = try(global_secondary_index.value.sort_key, null) == null ? [] : [global_secondary_index.value.sort_key]
        content {
          attribute_name = key_schema.value.name
          key_type       = "RANGE"
        }
      }
    }
  }

  point_in_time_recovery {
    enabled = false
  }

  tags = var.tags
}

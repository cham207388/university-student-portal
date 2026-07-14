variable "table_name" {
  description = "DynamoDB table name."
  type        = string
}

variable "partition_key" {
  description = "Table partition key definition."
  type = object({
    name = string
    type = string
  })
}

variable "sort_key" {
  description = "Optional table sort key definition."
  type = object({
    name = string
    type = string
  })
  default = null
}

variable "global_secondary_indexes" {
  description = "Global secondary indexes required by documented access patterns."
  type = list(object({
    name = string
    partition_key = object({
      name = string
      type = string
    })
    sort_key = optional(object({
      name = string
      type = string
    }))
  }))
  default = []
}

variable "tags" {
  description = "Tags applied to the DynamoDB table."
  type        = map(string)
  default     = {}
}

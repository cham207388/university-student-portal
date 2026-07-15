resource "aws_db_instance" "postgres" {
  identifier              = var.postgres_identifier
  engine                  = "postgres"
  engine_version          = "16"
  instance_class          = "db.t3.micro"
  allocated_storage       = 20
  storage_type            = "gp2"
  db_name                 = var.postgres_database
  username                = var.postgres_username
  password                = var.postgres_password
  port                    = 5432
  publicly_accessible     = true
  skip_final_snapshot     = true
  deletion_protection     = false
  apply_immediately       = true
  backup_retention_period = 0
  tags                    = merge(local.common_tags, { Domain = "postgres" })

  # LocalStack reports the externally mapped edge port (4510) after creation,
  # rather than the requested database listener port (5432). Ignoring that
  # emulator-only readback prevents a perpetual update and an inconsistent
  # Secrets Manager value during apply.
  lifecycle {
    ignore_changes = [port]
  }
}

resource "aws_secretsmanager_secret" "postgres" {
  name                    = "${var.postgres_identifier}/credentials"
  description             = "LocalStack RDS PostgreSQL credentials"
  recovery_window_in_days = 0
  tags                    = merge(local.common_tags, { Domain = "postgres" })
}

resource "aws_secretsmanager_secret_version" "postgres" {
  secret_id = aws_secretsmanager_secret.postgres.id
  secret_string = jsonencode({
    engine   = "postgres"
    host     = var.postgres_host
    port     = var.postgres_external_port
    database = var.postgres_database
    username = var.postgres_username
    password = var.postgres_password
  })
}

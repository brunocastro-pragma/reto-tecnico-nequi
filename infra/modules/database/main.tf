# manage_master_user_password: RDS generates the password, stores it in Secrets Manager and rotates
# it. Passing it in as a variable would leave it in plain text inside the Terraform state.

resource "aws_db_subnet_group" "this" {
  name       = "${var.name_prefix}-db-subnet-group"
  subnet_ids = var.private_subnet_ids

  tags = merge(var.tags, { Name = "${var.name_prefix}-db-subnet-group" })
}

resource "aws_db_instance" "this" {
  identifier     = "${var.name_prefix}-postgres"
  engine         = "postgres"
  engine_version = var.engine_version
  instance_class = var.instance_class

  allocated_storage     = var.allocated_storage
  max_allocated_storage = var.max_allocated_storage
  storage_type          = "gp3"
  storage_encrypted     = true

  db_name  = var.database_name
  username = var.master_username

  manage_master_user_password = true

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [var.database_security_group_id]
  # Never public. The only route to it is from the tasks' security group.
  publicly_accessible = false

  multi_az                  = var.multi_az
  backup_retention_period   = var.backup_retention_period
  skip_final_snapshot       = var.skip_final_snapshot
  final_snapshot_identifier = var.skip_final_snapshot ? null : "${var.name_prefix}-postgres-final"
  deletion_protection       = var.deletion_protection

  performance_insights_enabled = var.performance_insights_enabled
  auto_minor_version_upgrade   = true

  tags = merge(var.tags, { Name = "${var.name_prefix}-postgres" })
}

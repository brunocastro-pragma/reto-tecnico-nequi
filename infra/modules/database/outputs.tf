output "endpoint" {
  description = "host:port of the instance."
  value       = aws_db_instance.this.endpoint
}

output "host" {
  description = "Hostname only. Not a secret: it resolves to a private address nobody outside the VPC can reach."
  value       = aws_db_instance.this.address
}

output "port" {
  description = "Port the instance listens on."
  value       = aws_db_instance.this.port
}

output "database_name" {
  description = "Name of the database."
  value       = aws_db_instance.this.db_name
}

output "secret_arn" {
  description = "Secret managed and rotated by RDS. Two keys: username and password -- which is why host, port and dbname reach the task as environment variables."
  value       = aws_db_instance.this.master_user_secret[0].secret_arn
}

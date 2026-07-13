output "vpc_id" {
  description = "Id of the VPC."
  value       = aws_vpc.this.id
}

output "public_subnet_ids" {
  description = "Public subnets, for the load balancer."
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "Private subnets, for the tasks and the database."
  value       = aws_subnet.private[*].id
}

output "alb_security_group_id" {
  description = "Security group of the load balancer."
  value       = aws_security_group.alb.id
}

output "ecs_security_group_id" {
  description = "Security group of the application tasks."
  value       = aws_security_group.ecs.id
}

output "database_security_group_id" {
  description = "Security group of the database."
  value       = aws_security_group.database.id
}

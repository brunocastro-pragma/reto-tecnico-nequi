output "cluster_name" {
  description = "Name of the ECS cluster."
  value       = aws_ecs_cluster.this.name
}

output "service_name" {
  description = "Name of the ECS service. Needed to force a new deployment from CI."
  value       = aws_ecs_service.this.name
}

output "task_definition_arn" {
  description = "ARN of the task definition, including its revision."
  value       = aws_ecs_task_definition.this.arn
}

output "log_group_name" {
  description = "CloudWatch log group where the application logs land."
  value       = aws_cloudwatch_log_group.this.name
}

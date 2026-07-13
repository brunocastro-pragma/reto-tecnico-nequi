output "execution_role_arn" {
  description = "Role the ECS agent assumes to pull the image and read the secret."
  value       = aws_iam_role.execution.arn
}

output "task_role_arn" {
  description = "Role the application itself runs as."
  value       = aws_iam_role.task.arn
}

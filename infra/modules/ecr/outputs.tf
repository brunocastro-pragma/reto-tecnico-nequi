output "repository_url" {
  description = "URL to tag and push images to."
  value       = aws_ecr_repository.this.repository_url
}

output "repository_arn" {
  description = "ARN of the repository, used to scope the execution role's pull permissions."
  value       = aws_ecr_repository.this.arn
}

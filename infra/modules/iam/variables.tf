variable "name_prefix" {
  description = "Prefix applied to the name of every resource."
  type        = string
}

variable "ecr_repository_arn" {
  description = "ARN of the ECR repository the execution role may pull from."
  type        = string
}

variable "database_secret_arn" {
  description = "ARN of the single Secrets Manager secret both roles may read."
  type        = string
}

variable "log_group_name" {
  description = "Name of the CloudWatch log group the execution role may write to. The ARN is composed from it to avoid a dependency cycle with the ecs module."
  type        = string
}

variable "tags" {
  description = "Tags applied to every resource."
  type        = map(string)
  default     = {}
}

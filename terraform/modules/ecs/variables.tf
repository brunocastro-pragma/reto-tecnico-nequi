variable "name_prefix" {
  description = "Prefix applied to the name of every resource."
  type        = string
}

variable "aws_region" {
  description = "Region, needed by the awslogs driver and by the AWS SDK inside the container."
  type        = string
}

variable "image" {
  description = "Full image reference, including the tag. Never :latest -- see the ECR module."
  type        = string
}

variable "spring_profile" {
  description = "Spring profile the container runs with. Selects application-{profile}.yml."
  type        = string
  default     = "prod"
}

variable "container_name" {
  description = "Name of the container inside the task."
  type        = string
  default     = "franchise-api"
}

variable "app_port" {
  description = "Port the application listens on."
  type        = number
  default     = 8080
}

variable "task_cpu" {
  description = "CPU units for the task. 512 = 0.5 vCPU."
  type        = number
  default     = 512
}

variable "task_memory" {
  description = "Memory in MB. Fargate only accepts certain cpu/memory pairs."
  type        = number
  default     = 1024
}

variable "desired_count" {
  description = "Tasks to run when the service is created. Auto scaling takes over from here."
  type        = number
  default     = 2
}

variable "min_capacity" {
  description = "Floor for auto scaling. Two, so a single failing task never means an outage."
  type        = number
  default     = 2
}

variable "max_capacity" {
  description = "Ceiling for auto scaling. It also caps the bill if traffic goes wrong."
  type        = number
  default     = 6
}

variable "cpu_target_value" {
  description = "Average CPU percentage auto scaling aims to hold the fleet at."
  type        = number
  default     = 60
}

variable "memory_target_value" {
  description = "Average memory percentage auto scaling aims to hold the fleet at."
  type        = number
  default     = 75
}

variable "private_subnet_ids" {
  description = "Private subnets the tasks run in."
  type        = list(string)
}

variable "security_group_id" {
  description = "Security group of the tasks."
  type        = string
}

variable "target_group_arn" {
  description = "Target group the tasks register into."
  type        = string
}

variable "execution_role_arn" {
  description = "Role the ECS agent assumes to pull the image and read the secret."
  type        = string
}

variable "task_role_arn" {
  description = "Role the application runs as."
  type        = string
}

variable "database_host" {
  description = "Hostname of the RDS instance."
  type        = string
}

variable "database_port" {
  description = "Port of the RDS instance."
  type        = number
  default     = 5432
}

variable "database_name" {
  description = "Name of the database."
  type        = string
}

variable "database_secret_arn" {
  description = "ARN of the secret holding username and password. The value never leaves Secrets Manager."
  type        = string
}

variable "log_retention_days" {
  description = "How long CloudWatch keeps the logs."
  type        = number
  default     = 7
}

variable "container_insights" {
  description = "Enable Container Insights. Costs money; useful in production."
  type        = bool
  default     = false
}

variable "tags" {
  description = "Tags applied to every resource."
  type        = map(string)
  default     = {}
}

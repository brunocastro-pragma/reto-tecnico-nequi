variable "name_prefix" {
  description = "Prefix applied to the name of every resource."
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnets the instance lives in."
  type        = list(string)
}

variable "database_security_group_id" {
  description = "Security group that only allows traffic from the application tasks."
  type        = string
}

variable "database_name" {
  description = "Name of the database created on the instance."
  type        = string
  default     = "franchise"
}

variable "master_username" {
  description = "Master user. The password is not here on purpose: RDS generates it and stores it in Secrets Manager."
  type        = string
  default     = "franchise"
}

variable "engine_version" {
  description = "PostgreSQL major version."
  type        = string
  default     = "16"
}

variable "instance_class" {
  description = "Instance size. db.t4g.micro is enough for dev; production sizes up."
  type        = string
  default     = "db.t4g.micro"
}

variable "allocated_storage" {
  description = "Initial storage in GB."
  type        = number
  default     = 20
}

variable "max_allocated_storage" {
  description = "Ceiling for storage autoscaling in GB."
  type        = number
  default     = 50
}

variable "multi_az" {
  description = "Standby replica in a second AZ. Doubles the cost and is the difference between dev and prod."
  type        = bool
  default     = false
}

variable "backup_retention_period" {
  description = "Days of automated backups. Zero disables them."
  type        = number
  default     = 1
}

variable "skip_final_snapshot" {
  description = "Skip the snapshot taken on destroy. True for dev, false anywhere real."
  type        = bool
  default     = true
}

variable "deletion_protection" {
  description = "Refuse to destroy the instance. True in production."
  type        = bool
  default     = false
}

variable "performance_insights_enabled" {
  description = "Enable Performance Insights."
  type        = bool
  default     = false
}

variable "tags" {
  description = "Tags applied to every resource."
  type        = map(string)
  default     = {}
}

variable "project" {
  description = "Project name. Prefixes the bucket and the lock table."
  type        = string
  default     = "franchise-api"
}

variable "aws_region" {
  description = "Region the state bucket and lock table live in."
  type        = string
  default     = "us-east-1"
}

variable "tags" {
  description = "Tags applied to every resource."
  type        = map(string)
  default = {
    Project   = "franchise-api"
    ManagedBy = "terraform"
    Purpose   = "terraform-remote-state"
  }
}

variable "name_prefix" {
  description = "Prefix applied to the name of every resource."
  type        = string
}

variable "vpc_id" {
  description = "VPC the target group lives in."
  type        = string
}

variable "public_subnet_ids" {
  description = "Public subnets the load balancer is attached to."
  type        = list(string)
}

variable "security_group_id" {
  description = "Security group of the load balancer."
  type        = string
}

variable "app_port" {
  description = "Port the tasks listen on."
  type        = number
  default     = 8080
}

variable "health_check_path" {
  description = "Path the load balancer polls to decide whether a task is alive."
  type        = string
  default     = "/actuator/health"
}

variable "deletion_protection" {
  description = "Refuse to destroy the load balancer. True in production."
  type        = bool
  default     = false
}

variable "tags" {
  description = "Tags applied to every resource."
  type        = map(string)
  default     = {}
}

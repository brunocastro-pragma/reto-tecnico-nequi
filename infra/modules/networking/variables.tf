variable "name_prefix" {
  description = "Prefix applied to the name of every resource, so two environments never collide."
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block of the VPC."
  type        = string
  default     = "10.0.0.0/16"
}

variable "az_count" {
  description = "Number of availability zones to spread the subnets across. Two is the minimum an ALB accepts."
  type        = number
  default     = 2

  validation {
    condition     = var.az_count >= 2
    error_message = "An Application Load Balancer requires subnets in at least two availability zones."
  }
}

variable "single_nat_gateway" {
  description = "Share one NAT gateway across all AZs. Cheaper, but a single point of failure: true for dev, false for prod."
  type        = bool
  default     = true
}

variable "app_port" {
  description = "Port the application listens on inside the container."
  type        = number
  default     = 8080
}

variable "tags" {
  description = "Tags applied to every resource."
  type        = map(string)
  default     = {}
}

variable "project" {
  description = "Project name. Prefixes every resource."
  type        = string
  default     = "franchise-api"
}

variable "environment" {
  description = "Environment name. dev, staging or prod."
  type        = string
  default     = "dev"
}

variable "aws_region" {
  description = "Region everything is deployed to."
  type        = string
  default     = "us-east-1"
}

variable "vpc_cidr" {
  description = "CIDR block of the VPC."
  type        = string
  default     = "10.0.0.0/16"
}

variable "app_port" {
  description = "Port the application listens on."
  type        = number
  default     = 8080
}

variable "image_tag" {
  description = "Tag of the image to run. CI passes the commit sha, so a rollback is redeploying the previous tag. Never :latest."
  type        = string
}

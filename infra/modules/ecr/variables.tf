variable "repository_name" {
  description = "Name of the ECR repository."
  type        = string
}

variable "max_image_count" {
  description = "How many images to keep before the lifecycle policy expires the oldest."
  type        = number
  default     = 10
}

variable "tags" {
  description = "Tags applied to every resource."
  type        = map(string)
  default     = {}
}

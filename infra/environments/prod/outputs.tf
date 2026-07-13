output "api_url" {
  description = "Base URL of the deployed API."
  value       = "http://${module.alb.dns_name}"
}

output "swagger_url" {
  description = "Swagger UI of the deployed API."
  value       = "http://${module.alb.dns_name}/swagger-ui.html"
}

output "ecr_repository_url" {
  description = "Repository to push the image to before deploying."
  value       = module.ecr.repository_url
}

output "ecs_cluster_name" {
  description = "Cluster name, for `aws ecs update-service --force-new-deployment`."
  value       = module.ecs.cluster_name
}

output "ecs_service_name" {
  description = "Service name, for `aws ecs update-service --force-new-deployment`."
  value       = module.ecs.service_name
}

output "log_group_name" {
  description = "Where to read the application logs."
  value       = module.ecs.log_group_name
}

output "database_endpoint" {
  description = "Endpoint of the RDS instance. Reachable only from inside the VPC."
  value       = module.database.endpoint
}

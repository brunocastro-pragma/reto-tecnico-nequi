output "dns_name" {
  description = "Public hostname of the API. This is the URL the service answers on."
  value       = aws_lb.this.dns_name
}

output "target_group_arn" {
  description = "Target group the ECS service registers its tasks into."
  value       = aws_lb_target_group.this.arn
}

output "listener_arn" {
  description = "ARN of the HTTP listener. The ECS service depends on it so tasks are not started before the listener can route to them."
  value       = aws_lb_listener.http.arn
}

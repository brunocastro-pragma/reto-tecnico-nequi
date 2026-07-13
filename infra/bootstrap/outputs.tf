output "state_bucket_name" {
  description = "Copy this into the `bucket` field of each environment's backend.tf."
  value       = aws_s3_bucket.state.id
}

output "lock_table_name" {
  description = "Copy this into the `dynamodb_table` field of each environment's backend.tf."
  value       = aws_dynamodb_table.lock.name
}

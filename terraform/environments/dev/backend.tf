# Remote state in S3 with a DynamoDB lock: local state would live on one laptop, and two concurrent
# applies would corrupt each other. The key includes the environment, so dev and prod never
# overwrite one another.
#
# The bucket and the table come from terraform/bootstrap. Put the bucket name it outputs here, then
# run `terraform init`.
terraform {
  backend "s3" {
    bucket         = "franchise-api-tfstate-e2c4a5d0"
    key            = "dev/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "franchise-api-tflock"
    encrypt        = true
  }
}

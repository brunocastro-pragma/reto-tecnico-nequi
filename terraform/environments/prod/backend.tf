# Same bucket and same lock table as dev, different key: the state files are separate, so an apply
# in dev can never touch what runs in prod.
terraform {
  backend "s3" {
    bucket         = "franchise-api-tfstate-e2c4a5d0"
    key            = "prod/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "franchise-api-tflock"
    encrypt        = true
  }
}

project     = "franchise-api"
environment = "dev"
aws_region  = "us-east-1"
vpc_cidr    = "10.0.0.0/16"

# Overridden by CI with the commit sha: terraform apply -var="image_tag=$GITHUB_SHA"
image_tag = "1.2.0"

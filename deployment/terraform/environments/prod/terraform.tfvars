project     = "franchise-api"
environment = "prod"
aws_region  = "us-east-1"

# A different CIDR from dev on purpose: identical ranges would make the two VPCs impossible to
# peer, and one day somebody will want to.
vpc_cidr = "10.1.0.0/16"

image_tag = "1.0.0"

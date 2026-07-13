# dev: small instances, one shared NAT, no deletion protection. prod calls the same modules with
# different numbers.

terraform {
  required_version = ">= 1.7"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = local.tags
  }
}

locals {
  name_prefix = "${var.project}-${var.environment}"

  tags = {
    Project     = var.project
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

module "networking" {
  source = "../../modules/networking"

  name_prefix        = local.name_prefix
  vpc_cidr           = var.vpc_cidr
  az_count           = 2
  single_nat_gateway = true
  app_port           = var.app_port
  tags               = local.tags
}

module "ecr" {
  source = "../../modules/ecr"

  repository_name = var.project
  max_image_count = 10
  tags            = local.tags
}

module "database" {
  source = "../../modules/database"

  name_prefix                = local.name_prefix
  private_subnet_ids         = module.networking.private_subnet_ids
  database_security_group_id = module.networking.database_security_group_id

  instance_class          = "db.t4g.micro"
  multi_az                = false
  backup_retention_period = 1
  skip_final_snapshot     = true
  deletion_protection     = false

  tags = local.tags
}

module "iam" {
  source = "../../modules/iam"

  name_prefix         = local.name_prefix
  ecr_repository_arn  = module.ecr.repository_arn
  database_secret_arn = module.database.secret_arn
  log_group_name      = "/ecs/${local.name_prefix}"
  tags                = local.tags
}

module "alb" {
  source = "../../modules/alb"

  name_prefix       = local.name_prefix
  vpc_id            = module.networking.vpc_id
  public_subnet_ids = module.networking.public_subnet_ids
  security_group_id = module.networking.alb_security_group_id
  app_port          = var.app_port
  tags              = local.tags
}

module "ecs" {
  source = "../../modules/ecs"

  name_prefix = local.name_prefix
  aws_region  = var.aws_region
  image       = "${module.ecr.repository_url}:${var.image_tag}"
  app_port    = var.app_port

  task_cpu      = 512
  task_memory   = 1024
  desired_count = 2
  min_capacity  = 2
  max_capacity  = 4

  private_subnet_ids = module.networking.private_subnet_ids
  security_group_id  = module.networking.ecs_security_group_id
  target_group_arn   = module.alb.target_group_arn

  execution_role_arn = module.iam.execution_role_arn
  task_role_arn      = module.iam.task_role_arn

  database_host       = module.database.host
  database_port       = module.database.port
  database_name       = module.database.database_name
  database_secret_arn = module.database.secret_arn

  log_retention_days = 7
  tags               = local.tags

  # The target group alone is not enough: without the listener the tasks have nowhere to receive
  # traffic from, and the service reports itself healthy while the ALB answers 503.
  depends_on = [module.alb]
}

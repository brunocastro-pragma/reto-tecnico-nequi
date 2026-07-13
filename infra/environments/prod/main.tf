# prod: same modules as dev, different numbers -- a NAT per AZ, Multi-AZ RDS with 7 days of
# backups, deletion protection, bigger tasks and a higher scaling ceiling.
#
# This is why the environments are directories and not workspaces: prod is not dev with another
# name, it has different topology, and a workspace would express that through conditionals on
# terraform.workspace scattered across the code.

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
  single_nat_gateway = false
  app_port           = var.app_port
  tags               = local.tags
}

module "ecr" {
  source = "../../modules/ecr"

  repository_name = var.project
  max_image_count = 30
  tags            = local.tags
}

module "database" {
  source = "../../modules/database"

  name_prefix                = local.name_prefix
  private_subnet_ids         = module.networking.private_subnet_ids
  database_security_group_id = module.networking.database_security_group_id

  instance_class               = "db.t4g.small"
  allocated_storage            = 50
  max_allocated_storage        = 200
  multi_az                     = true
  backup_retention_period      = 7
  skip_final_snapshot          = false
  deletion_protection          = true
  performance_insights_enabled = true

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

  name_prefix         = local.name_prefix
  vpc_id              = module.networking.vpc_id
  public_subnet_ids   = module.networking.public_subnet_ids
  security_group_id   = module.networking.alb_security_group_id
  app_port            = var.app_port
  deletion_protection = true
  tags                = local.tags
}

module "ecs" {
  source = "../../modules/ecs"

  name_prefix = local.name_prefix
  aws_region  = var.aws_region
  image       = "${module.ecr.repository_url}:${var.image_tag}"
  app_port    = var.app_port

  task_cpu      = 1024
  task_memory   = 2048
  desired_count = 3
  min_capacity  = 3
  max_capacity  = 12

  private_subnet_ids = module.networking.private_subnet_ids
  security_group_id  = module.networking.ecs_security_group_id
  target_group_arn   = module.alb.target_group_arn

  execution_role_arn = module.iam.execution_role_arn
  task_role_arn      = module.iam.task_role_arn

  database_host       = module.database.host
  database_port       = module.database.port
  database_name       = module.database.database_name
  database_secret_arn = module.database.secret_arn

  log_retention_days = 30
  container_insights = true
  tags               = local.tags

  depends_on = [module.alb]
}

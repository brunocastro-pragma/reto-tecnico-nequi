# Two roles, used at two different moments:
#   execution role -- the ECS agent, before the container starts: pull the image, read the secret,
#                     create the log stream.
#   task role      -- the application itself, at runtime.
# The application never pulls images, so its role has no ECR permission at all.

# The log group belongs to the ecs module, which in turn needs the role ARNs from here.
# Referencing the resource would be a cycle, so the ARN is composed from values known up front.
# It still names one log group, not "*".
data "aws_caller_identity" "current" {}

data "aws_region" "current" {}

locals {
  # .name, not .region: the environments pin the AWS provider to ~> 5.0, where `region` does not
  # exist yet.
  log_group_arn = "arn:aws:logs:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:log-group:${var.log_group_name}"
}

data "aws_iam_policy_document" "ecs_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

# --- Execution role ----------------------------------------------------------

resource "aws_iam_role" "execution" {
  name               = "${var.name_prefix}-ecs-execution-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume_role.json
  tags               = var.tags
}

data "aws_iam_policy_document" "execution" {
  # GetAuthorizationToken is account-wide, so AWS only accepts "*". The statements below, which
  # actually read image content, are scoped to the one repository.
  statement {
    sid       = "EcrAuth"
    effect    = "Allow"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid    = "EcrPullThisRepositoryOnly"
    effect = "Allow"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:GetDownloadUrlForLayer",
      "ecr:BatchGetImage",
    ]
    resources = [var.ecr_repository_arn]
  }

  statement {
    sid    = "WriteItsOwnLogsOnly"
    effect = "Allow"
    actions = [
      "logs:CreateLogStream",
      "logs:PutLogEvents",
    ]
    resources = ["${local.log_group_arn}:*"]
  }

  # One secret, by ARN.
  statement {
    sid       = "ReadTheDatabaseSecretOnly"
    effect    = "Allow"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = [var.database_secret_arn]
  }
}

resource "aws_iam_role_policy" "execution" {
  name   = "${var.name_prefix}-ecs-execution-policy"
  role   = aws_iam_role.execution.id
  policy = data.aws_iam_policy_document.execution.json
}

# --- Task role ---------------------------------------------------------------

resource "aws_iam_role" "task" {
  name               = "${var.name_prefix}-ecs-task-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume_role.json
  tags               = var.tags
}

# The application reads its secret at startup through the Spring Cloud AWS starter and talks to
# nothing else in AWS, so GetSecretValue on that ARN is the whole policy.
data "aws_iam_policy_document" "task" {
  statement {
    sid       = "ReadTheDatabaseSecretOnly"
    effect    = "Allow"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = [var.database_secret_arn]
  }
}

resource "aws_iam_role_policy" "task" {
  name   = "${var.name_prefix}-ecs-task-policy"
  role   = aws_iam_role.task.id
  policy = data.aws_iam_policy_document.task.json
}

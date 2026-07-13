resource "aws_cloudwatch_log_group" "this" {
  name              = "/ecs/${var.name_prefix}"
  retention_in_days = var.log_retention_days

  tags = var.tags
}

resource "aws_ecs_cluster" "this" {
  name = "${var.name_prefix}-cluster"

  setting {
    name  = "containerInsights"
    value = var.container_insights ? "enabled" : "disabled"
  }

  tags = var.tags
}

# Fargate: no instances to patch or right-size, billed per vCPU-second. EC2 would be cheaper under
# sustained load and would allow a warm pool, at the cost of operating the fleet.
resource "aws_ecs_task_definition" "this" {
  family                   = var.name_prefix
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  execution_role_arn       = var.execution_role_arn
  task_role_arn            = var.task_role_arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([{
    name      = var.container_name
    image     = var.image
    essential = true

    portMappings = [{
      containerPort = var.app_port
      protocol      = "tcp"
    }]

    # No secrets: the endpoint of a database in a private subnet is not a credential, and the ARN
    # is a pointer the application resolves against Secrets Manager at startup.
    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "aws" },
      { name = "AWS_REGION", value = var.aws_region },
      { name = "DB_HOST", value = var.database_host },
      { name = "DB_PORT", value = tostring(var.database_port) },
      { name = "DB_NAME", value = var.database_name },
      { name = "DB_SECRET_ARN", value = var.database_secret_arn },
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.this.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "ecs"
      }
    }

    healthCheck = {
      command  = ["CMD-SHELL", "wget -q --spider http://localhost:${var.app_port}/actuator/health || exit 1"]
      interval = 30
      timeout  = 5
      retries  = 3
      # Without a start period ECS kills the task while the JVM is still booting -- forever.
      startPeriod = 60
    }
  }])

  tags = var.tags
}

resource "aws_ecs_service" "this" {
  name            = "${var.name_prefix}-service"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.this.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [var.security_group_id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = var.target_group_arn
    container_name   = var.container_name
    container_port   = var.app_port
  }

  # 100/200: the new tasks pass their health check before the old ones drain, so the service never
  # dips below the desired count during a deploy.
  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  deployment_circuit_breaker {
    enable = true
    # A deploy whose tasks never turn healthy rolls itself back instead of crash-looping.
    rollback = true
  }

  health_check_grace_period_seconds = 90

  tags = var.tags
}

# --- Auto scaling ------------------------------------------------------------

resource "aws_appautoscaling_target" "this" {
  service_namespace  = "ecs"
  resource_id        = "service/${aws_ecs_cluster.this.name}/${aws_ecs_service.this.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  min_capacity       = var.min_capacity
  max_capacity       = var.max_capacity
}

# Target tracking rather than step scaling: declare the CPU level the fleet should sit at and let
# AWS work out the task count. 60% leaves headroom for the time a new task takes to boot.
resource "aws_appautoscaling_policy" "cpu" {
  name               = "${var.name_prefix}-cpu-target-tracking"
  policy_type        = "TargetTrackingScaling"
  service_namespace  = aws_appautoscaling_target.this.service_namespace
  resource_id        = aws_appautoscaling_target.this.resource_id
  scalable_dimension = aws_appautoscaling_target.this.scalable_dimension

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }

    target_value = var.cpu_target_value
    # Out fast, in slowly: an unnecessary task costs cents, removing a needed one costs latency.
    scale_out_cooldown = 60
    scale_in_cooldown  = 300
  }
}

resource "aws_appautoscaling_policy" "memory" {
  name               = "${var.name_prefix}-memory-target-tracking"
  policy_type        = "TargetTrackingScaling"
  service_namespace  = aws_appautoscaling_target.this.service_namespace
  resource_id        = aws_appautoscaling_target.this.resource_id
  scalable_dimension = aws_appautoscaling_target.this.scalable_dimension

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageMemoryUtilization"
    }

    target_value       = var.memory_target_value
    scale_out_cooldown = 60
    scale_in_cooldown  = 300
  }
}

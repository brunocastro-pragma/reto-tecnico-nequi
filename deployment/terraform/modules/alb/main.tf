resource "aws_lb" "this" {
  name               = "${var.name_prefix}-alb"
  load_balancer_type = "application"
  internal           = false
  subnets            = var.public_subnet_ids
  security_groups    = [var.security_group_id]

  enable_deletion_protection = var.deletion_protection
  idle_timeout               = 60

  tags = merge(var.tags, { Name = "${var.name_prefix}-alb" })
}

resource "aws_lb_target_group" "this" {
  name     = "${var.name_prefix}-tg"
  port     = var.app_port
  protocol = "HTTP"
  vpc_id   = var.vpc_id
  # "ip", not "instance": Fargate tasks have no EC2 instance to register, they get an ENI in the
  # subnet and are targeted by their private IP.
  target_type = "ip"

  health_check {
    enabled = true
    path    = var.health_check_path
    matcher = "200"
    # An unhealthy task is pulled out in about 90s. Tighter than this and a GC pause could evict
    # a healthy task mid-request.
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  # Give in-flight requests 30s to finish before the target is killed during a deploy.
  deregistration_delay = 30

  tags = merge(var.tags, { Name = "${var.name_prefix}-tg" })
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.this.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.this.arn
  }
}

# No HTTPS listener: it needs a domain and an ACM certificate. With one, this listener would
# redirect to 443 and the module would take a certificate_arn.

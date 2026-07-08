locals {
  name_prefix = "missionmatch-prod"
}

module "network" {
  source = "../../modules/network"

  name_prefix        = local.name_prefix
  availability_zones = var.availability_zones
  single_nat_gateway = false # prod: one NAT per AZ, so a single AZ outage doesn't take routing down with it
}

resource "aws_security_group" "ecs_tasks" {
  name_prefix = "${local.name_prefix}-ecs-"
  description = "MissionMatch ECS tasks"
  vpc_id      = module.network.vpc_id

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_vpc_security_group_egress_rule" "ecs_tasks_all" {
  security_group_id = aws_security_group.ecs_tasks.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

module "rds" {
  source = "../../modules/rds"

  name_prefix           = local.name_prefix
  vpc_id                = module.network.vpc_id
  private_subnet_ids    = module.network.private_subnet_ids
  ecs_security_group_id = aws_security_group.ecs_tasks.id
  instance_class        = "db.t4g.small"
  multi_az              = true
  deletion_protection   = true
  skip_final_snapshot   = false
  backup_retention_days = 7
}

module "msk" {
  source = "../../modules/msk"

  name_prefix           = local.name_prefix
  vpc_id                = module.network.vpc_id
  private_subnet_ids    = module.network.private_subnet_ids
  ecs_security_group_id = aws_security_group.ecs_tasks.id
}

module "ecs_service" {
  source = "../../modules/ecs-service"

  name_prefix           = local.name_prefix
  vpc_id                = module.network.vpc_id
  public_subnet_ids     = module.network.public_subnet_ids
  private_subnet_ids    = module.network.private_subnet_ids
  ecs_security_group_id = aws_security_group.ecs_tasks.id

  container_image = var.container_image
  cpu             = 1024
  memory          = 2048
  desired_count   = 2 # survives one task being drained/replaced without downtime

  kafka_cluster_arn = module.msk.cluster_arn
  certificate_arn   = var.acm_certificate_arn_regional

  environment_variables = {
    SPRING_PROFILES_ACTIVE         = "aws"
    SPRING_DATASOURCE_URL          = "jdbc:postgresql://${module.rds.db_endpoint}:${module.rds.db_port}/${module.rds.db_name}"
    SPRING_KAFKA_BOOTSTRAP_SERVERS = module.msk.bootstrap_brokers_iam
  }

  secrets = {
    SPRING_DATASOURCE_USERNAME = "${module.rds.credentials_secret_arn}:username::"
    SPRING_DATASOURCE_PASSWORD = "${module.rds.credentials_secret_arn}:password::"
  }
}

module "frontend_hosting" {
  source = "../../modules/frontend-hosting"

  name_prefix         = local.name_prefix
  api_alb_dns_name    = module.ecs_service.alb_dns_name
  domain_name         = var.domain_name
  hosted_zone_id      = var.hosted_zone_id
  acm_certificate_arn = var.acm_certificate_arn_us_east_1
  price_class         = "PriceClass_All"
}

module "observability" {
  source = "../../modules/observability"

  name_prefix             = local.name_prefix
  cluster_name            = module.ecs_service.cluster_name
  service_name            = module.ecs_service.service_name
  alb_arn_suffix          = module.ecs_service.alb_arn_suffix
  target_group_arn_suffix = module.ecs_service.target_group_arn_suffix
  alert_email             = var.alert_email
}

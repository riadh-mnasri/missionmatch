output "ecr_repository_url" {
  value = module.ecs_service.ecr_repository_url
}

output "alb_dns_name" {
  value = module.ecs_service.alb_dns_name
}

output "frontend_url" {
  value = "https://${module.frontend_hosting.distribution_domain_name}"
}

output "frontend_bucket_name" {
  value = module.frontend_hosting.bucket_name
}

output "db_endpoint" {
  value = module.rds.db_endpoint
}

output "kafka_bootstrap_brokers" {
  value = module.msk.bootstrap_brokers_iam
}

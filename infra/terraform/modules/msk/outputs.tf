output "cluster_arn" {
  value = aws_msk_serverless_cluster.this.arn
}

output "bootstrap_brokers_iam" {
  description = "Bootstrap broker string for SASL/IAM clients"
  value       = data.aws_msk_bootstrap_brokers.this.bootstrap_brokers_sasl_iam
}

output "security_group_id" {
  value = aws_security_group.kafka.id
}

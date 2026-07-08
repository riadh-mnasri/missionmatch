output "sns_topic_arn" {
  value = var.alert_email == null ? null : aws_sns_topic.alerts[0].arn
}

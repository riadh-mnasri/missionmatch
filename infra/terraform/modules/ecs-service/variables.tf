variable "name_prefix" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "public_subnet_ids" {
  description = "Subnets for the internet-facing ALB"
  type        = list(string)
}

variable "private_subnet_ids" {
  description = "Subnets for the ECS tasks themselves (awsvpc mode, no public IP)"
  type        = list(string)
}

variable "ecs_security_group_id" {
  description = "Security group attached to the ECS tasks, created by the environment root so rds/msk can reference it without a module cycle"
  type        = string
}

variable "container_image" {
  description = "Full ECR image URI:tag for the bootstrap Spring Boot app"
  type        = string
}

variable "container_port" {
  type    = number
  default = 8181
}

variable "cpu" {
  description = "Fargate task CPU units (256 = .25 vCPU)"
  type        = number
  default     = 512
}

variable "memory" {
  description = "Fargate task memory in MiB"
  type        = number
  default     = 1024
}

variable "desired_count" {
  type    = number
  default = 1
}

variable "health_check_path" {
  type    = string
  default = "/actuator/health"
}

variable "environment_variables" {
  description = "Plain (non-secret) environment variables for the container"
  type        = map(string)
  default     = {}
}

variable "secrets" {
  description = "Environment variable name to Secrets Manager ARN, injected by ECS at container start"
  type        = map(string)
  default     = {}
}

variable "kafka_cluster_arn" {
  description = "MSK Serverless cluster ARN the task role needs kafka-cluster:* permissions on. Null skips the policy (useful before MSK exists yet)."
  type        = string
  default     = null
}

variable "certificate_arn" {
  description = "ACM certificate for the HTTPS listener. Null keeps the ALB on HTTP only, which is fine for a dev stack without a domain yet."
  type        = string
  default     = null
}

variable "log_retention_days" {
  type    = number
  default = 14
}

variable "tags" {
  type    = map(string)
  default = {}
}

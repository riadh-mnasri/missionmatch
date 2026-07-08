variable "aws_region" {
  type    = string
  default = "eu-west-1"
}

variable "availability_zones" {
  type    = list(string)
  default = ["eu-west-1a", "eu-west-1b"]
}

variable "container_image" {
  description = "ECR image URI:tag for the bootstrap Spring Boot app, e.g. 123456789012.dkr.ecr.eu-west-1.amazonaws.com/missionmatch:latest. Build it with backend/Dockerfile."
  type        = string
}

variable "alert_email" {
  description = "Optional email to subscribe to CloudWatch alarms. Null skips SNS."
  type        = string
  default     = null
}

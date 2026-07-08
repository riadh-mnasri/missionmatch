variable "aws_region" {
  type    = string
  default = "eu-west-1"
}

variable "availability_zones" {
  type    = list(string)
  default = ["eu-west-1a", "eu-west-1b"]
}

variable "container_image" {
  description = "ECR image URI:tag for the bootstrap Spring Boot app. Build it with backend/Dockerfile."
  type        = string
}

variable "alert_email" {
  type    = string
  default = null
}

variable "domain_name" {
  description = "Custom domain for the frontend, e.g. app.missionmatch.dev. Null keeps the default *.cloudfront.net domain - true for this reference project until it has a real domain to put in front of it."
  type        = string
  default     = null
}

variable "hosted_zone_id" {
  type    = string
  default = null
}

variable "acm_certificate_arn_regional" {
  description = "Certificate for the ALB's HTTPS listener, in the same region as this stack (var.aws_region)."
  type        = string
  default     = null
}

variable "acm_certificate_arn_us_east_1" {
  description = "Certificate for CloudFront, which only ever accepts certificates from us-east-1 regardless of which region the rest of the stack runs in - a CloudFront-specific requirement, not a choice made here."
  type        = string
  default     = null
}

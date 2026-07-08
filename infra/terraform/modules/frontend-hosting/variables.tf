variable "name_prefix" {
  type = string
}

variable "api_alb_dns_name" {
  description = "ALB DNS name to proxy /api/* to, so the Angular app and the API share one origin from the browser's point of view - see the README section on why /api exists. Null serves the frontend standalone."
  type        = string
  default     = null
}

variable "domain_name" {
  description = "Custom domain for the distribution, e.g. app.missionmatch.dev. Null keeps the default *.cloudfront.net domain."
  type        = string
  default     = null
}

variable "hosted_zone_id" {
  description = "Route53 hosted zone to create the alias record in. Required if domain_name is set."
  type        = string
  default     = null
}

variable "acm_certificate_arn" {
  description = "ACM certificate ARN, must exist in us-east-1 regardless of the stack's region (a CloudFront requirement). Required if domain_name is set. Requesting and validating the certificate is left to the caller since DNS validation needs a provider aliased to us-east-1."
  type        = string
  default     = null
}

variable "price_class" {
  description = "PriceClass_100 (US/EU/Canada only) is cheapest and enough for a dev/demo audience; PriceClass_All adds edge locations worldwide for prod."
  type        = string
  default     = "PriceClass_100"
}

variable "tags" {
  type    = map(string)
  default = {}
}

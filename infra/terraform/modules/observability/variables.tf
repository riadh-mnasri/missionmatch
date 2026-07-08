variable "name_prefix" {
  type = string
}

variable "cluster_name" {
  type = string
}

variable "service_name" {
  type = string
}

variable "alb_arn_suffix" {
  type = string
}

variable "target_group_arn_suffix" {
  type = string
}

variable "cpu_threshold_percent" {
  type    = number
  default = 80
}

variable "memory_threshold_percent" {
  type    = number
  default = 80
}

variable "alb_5xx_threshold" {
  description = "Number of 5xx responses in one 5-minute period that triggers the alarm"
  type        = number
  default     = 10
}

variable "alert_email" {
  description = "If set, creates an SNS topic and subscribes this address to every alarm. Null skips SNS entirely - the alarms still exist and are visible in the console."
  type        = string
  default     = null
}

variable "tags" {
  type    = map(string)
  default = {}
}

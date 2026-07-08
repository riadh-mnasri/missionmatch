variable "name_prefix" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "private_subnet_ids" {
  description = "At least two subnets, in different AZs, for the MSK Serverless ENIs"
  type        = list(string)
}

variable "ecs_security_group_id" {
  description = "Security group of the ECS tasks allowed to reach Kafka"
  type        = string
}

variable "tags" {
  type    = map(string)
  default = {}
}

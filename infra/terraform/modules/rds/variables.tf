variable "name_prefix" {
  description = "Prefix applied to every resource name and tag"
  type        = string
}

variable "vpc_id" {
  type = string
}

variable "private_subnet_ids" {
  description = "Subnets the DB subnet group spans. Must have no route to an internet gateway."
  type        = list(string)
}

variable "ecs_security_group_id" {
  description = "Security group of the ECS tasks allowed to reach Postgres on 5432"
  type        = string
}

variable "engine_version" {
  description = "Postgres major.minor version, matching the postgres:16-alpine image used in docker-compose.yml"
  type        = string
  default     = "16.4"
}

variable "instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "allocated_storage" {
  description = "Storage in GiB"
  type        = number
  default     = 20
}

variable "db_name" {
  type    = string
  default = "missionmatch"
}

variable "master_username" {
  type    = string
  default = "missionmatch"
}

variable "multi_az" {
  description = "Run a synchronous standby in a second AZ. Doubles the RDS cost; enable for prod, not dev."
  type        = bool
  default     = false
}

variable "deletion_protection" {
  type    = bool
  default = false
}

variable "skip_final_snapshot" {
  description = "Skip the final snapshot on destroy. Fine for a disposable dev stack, never for prod."
  type        = bool
  default     = true
}

variable "backup_retention_days" {
  type    = number
  default = 1
}

variable "tags" {
  type    = map(string)
  default = {}
}

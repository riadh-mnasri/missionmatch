variable "name_prefix" {
  description = "Prefix applied to every resource name and tag, e.g. \"missionmatch-dev\""
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.20.0.0/16"
}

variable "availability_zones" {
  description = "Availability zones to spread subnets across. Two is enough for an ALB and RDS Multi-AZ; a learning environment doesn't need three."
  type        = list(string)
}

variable "single_nat_gateway" {
  description = "Use one NAT gateway shared by every private subnet instead of one per AZ. Cuts the hourly NAT cost roughly in half; acceptable for dev, not for prod where an AZ outage would take routing down with it."
  type        = bool
  default     = true
}

variable "tags" {
  description = "Tags applied to every resource created by this module"
  type        = map(string)
  default     = {}
}

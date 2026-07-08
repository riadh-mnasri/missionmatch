bucket         = "missionmatch-terraform-state"
key            = "prod/terraform.tfstate"
region         = "eu-west-1"
dynamodb_table = "missionmatch-terraform-locks"
encrypt        = true

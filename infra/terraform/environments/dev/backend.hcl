bucket         = "missionmatch-terraform-state"
key            = "dev/terraform.tfstate"
region         = "eu-west-1"
dynamodb_table = "missionmatch-terraform-locks"
encrypt        = true

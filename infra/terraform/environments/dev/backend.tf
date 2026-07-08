# Partial config on purpose: the bucket/key/region/table differ per environment, so dev and
# prod share this same block and supply the concrete values with:
#   terraform init -backend-config=backend.hcl
# The bucket and DynamoDB table themselves are created once by ../../bootstrap (plain local
# state - state for the state store can't live in the state store).
terraform {
  backend "s3" {}
}

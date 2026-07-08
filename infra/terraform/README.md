# MissionMatch infrastructure

Terraform for the AWS resources described in the main [README](../../README.md#infrastructure--deployment-aws--terraform):
one `network`, one `rds` Postgres instance, one `msk` Serverless cluster, one `ecs-service`
running the Spring Boot container, `frontend-hosting` for the built Angular app, and
`observability` alarms - composed differently by `environments/dev` and `environments/prod`.

Nothing here has been applied yet. This is real, `terraform validate`-clean infrastructure
code, written and reviewed like any other part of this reference project, but turning it into
running AWS resources is a separate, deliberate step - it costs real money and touches a real
AWS account, so it isn't something to run as a side effect of writing the code.

## One-time setup, per AWS account

Remote state needs somewhere to live before any environment can be initialized:

```
cd bootstrap
terraform init
terraform apply
```

This creates the S3 bucket and DynamoDB table that `environments/dev` and `environments/prod`
both point at (see their `backend.hcl`). It keeps its own state locally in `bootstrap/` - state
for the state store can't live in the state store.

## Deploying an environment

```
cd environments/dev
terraform init -backend-config=backend.hcl
cp terraform.tfvars.example terraform.tfvars   # fill in aws_region, container_image, ...
terraform plan
terraform apply
```

The `container_image` variable has a chicken-and-egg wrinkle worth knowing about: the ECR
repository it should point to is created *by* this stack (`module.ecs_service`), so it can't
exist before the first `apply`. `terraform.tfvars.example` defaults to a public placeholder
image so the first apply succeeds with an ECS service that starts but serves nothing useful;
build and push the real image with `backend/Dockerfile`, read the repository URL back from
`terraform output ecr_repository_url`, and `terraform apply` again with the real
`container_image` value.

`environments/prod` follows the same flow with a larger, Multi-AZ-hardened footprint - see the
module calls in `environments/prod/main.tf` for exactly what differs from dev.

## What isn't wired up yet

- **Kafka client auth**: MSK Serverless mandates SASL/IAM. The `aws` Spring profile
  (`backend/bootstrap/src/main/resources/application-aws.yml`) and the `aws-msk-iam-auth`
  dependency exist for this, but it has only been exercised via `terraform validate` and code
  review, not against a real MSK Serverless cluster.
- **CI/CD**: nothing here builds the Docker image, pushes to ECR, or runs `terraform apply`
  automatically yet. The main README's "State is remote... CI runs `terraform plan` on every
  pull request" describes the target, not the current state.
- **A real domain**: `environments/prod` accepts `domain_name` / `hosted_zone_id` /
  `acm_certificate_arn_*` but they default to `null`, meaning prod is reachable at its
  `*.cloudfront.net` and ALB DNS names until this project has a domain to put in front of it.

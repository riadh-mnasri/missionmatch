# MSK Serverless: no broker count, no storage sizing, no version upgrades to plan for.
# The tradeoff for a reference project is exactly right - it demonstrates real Kafka on AWS
# without turning this into a broker-capacity-planning exercise.
#
# Serverless mandates IAM client authentication (SASL/IAM); there is no way to opt into
# plaintext or SCRAM here, which is also the AWS-recommended posture.

resource "aws_security_group" "kafka" {
  name_prefix = "${var.name_prefix}-kafka-"
  description = "Kafka (IAM auth) access for the MissionMatch ECS service only"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Kafka IAM-auth bootstrap from ECS tasks"
    from_port       = 9098
    to_port         = 9098
    protocol        = "tcp"
    security_groups = [var.ecs_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.tags, { Name = "${var.name_prefix}-kafka" })

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_msk_serverless_cluster" "this" {
  cluster_name = "${var.name_prefix}-kafka"

  vpc_config {
    subnet_ids         = var.private_subnet_ids
    security_group_ids = [aws_security_group.kafka.id]
  }

  client_authentication {
    sasl {
      iam {
        enabled = true
      }
    }
  }

  tags = var.tags
}

data "aws_msk_bootstrap_brokers" "this" {
  cluster_arn = aws_msk_serverless_cluster.this.arn
}

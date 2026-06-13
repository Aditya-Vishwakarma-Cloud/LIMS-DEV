provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "LIMS"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

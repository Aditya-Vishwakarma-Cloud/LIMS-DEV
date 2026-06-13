# LIMS Infrastructure - Terraform

This directory contains the Terraform configuration to provision the AWS infrastructure for the LIMS project.

## Prerequisites

- Terraform installed (>= 1.5.0)
- AWS CLI configured with appropriate credentials
- An existing AWS Key Pair named `AWS-Key` in the `ap-south-1` region (or matching the region you deploy to).

## Architecture

- **VPC Module:** Creates a custom VPC and public subnet.
- **Security Group Module:** Allows inbound traffic for SSH (22), HTTP (80), HTTPS (443), Spring Boot (8080), and Prometheus (9090).
- **EC2 Module:** Provisions an Ubuntu 22.04 LTS instance with 25GB gp3 storage.

## Execution Order

1. **Initialize Terraform:**
   This will download the required providers and prepare the working directory.
   ```bash
   terraform init
   ```

2. **Plan the Deployment:**
   To see what resources will be created for a specific environment (e.g., `dev`):
   ```bash
   terraform plan -var-file="environments/dev/dev.tfvars"
   ```

3. **Apply the Configuration:**
   To provision the infrastructure:
   ```bash
   terraform apply -var-file="environments/dev/dev.tfvars"
   ```
   *Note: Terraform will automatically generate the Ansible inventory file (e.g., `dev.ini`) inside the `../ansible/inventory/` directory using the new EC2 instance's public IP.*

4. **Destroy the Infrastructure:**
   When you no longer need the resources:
   ```bash
   terraform destroy -var-file="environments/dev/dev.tfvars"
   ```

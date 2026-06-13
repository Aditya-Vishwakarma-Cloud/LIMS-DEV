module "vpc" {
  source             = "./modules/vpc"
  environment        = var.environment
  vpc_cidr           = var.vpc_cidr
  public_subnet_cidr = var.public_subnet_cidr
}

module "security_group" {
  source      = "./modules/security-group"
  environment = var.environment
  vpc_id      = module.vpc.vpc_id
}

module "ec2" {
  source            = "./modules/ec2"
  environment       = var.environment
  instance_name     = var.instance_name
  instance_type     = var.instance_type
  key_name          = var.key_name
  volume_size       = var.volume_size
  subnet_id         = module.vpc.public_subnet_id
  security_group_id = module.security_group.security_group_id
}

# Dynamically generate Ansible inventory file for the environment
resource "local_file" "ansible_inventory" {
  content = templatefile("${path.module}/templates/inventory.tftpl", {
    public_ip = module.ec2.public_ip
  })
  filename = "${path.module}/../ansible/inventory/${var.environment}.ini"
}

output "public_ip" {
  description = "Public IP of the instance"
  value       = aws_instance.lims_server.public_ip
}

output "public_dns" {
  description = "Public DNS of the instance"
  value       = aws_instance.lims_server.public_dns
}

output "instance_id" {
  description = "ID of the instance"
  value       = aws_instance.lims_server.id
}

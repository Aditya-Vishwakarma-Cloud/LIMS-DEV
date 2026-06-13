#!/bin/bash

# Exit immediately if a command exits with a non-zero status
# Treat unset variables as an error
# Ensure pipelines fail if any command within fails
set -euo pipefail

# Logger functions
log_info() {
    echo -e "\n[INFO] $1"
}

log_error() {
    echo -e "\n[ERROR] $1" >&2
}

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

log_info "Starting LIMS Server Setup..."

# Update package index if needed
log_info "Updating apt package index..."
sudo apt-get update -y

# Install prerequisites
log_info "Installing prerequisites..."
sudo apt-get install -y ca-certificates curl gnupg lsb-release apt-transport-https

# 1. Docker Installation (Official Repository)
if command_exists docker; then
    log_info "Docker is already installed. Skipping installation."
else
    log_info "Installing Docker Engine from official repository..."

    # Remove conflicting packages if they exist
    for pkg in docker.io docker-doc docker-compose docker-compose-v2 podman-docker containerd runc; do
        sudo apt-get remove -y $pkg || true
    done

    # Add Docker's official GPG key
    sudo install -m 0755 -d /etc/apt/keyrings
    if [ ! -f /etc/apt/keyrings/docker.asc ]; then
        sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
        sudo chmod a+r /etc/apt/keyrings/docker.asc
    fi

    # Add the repository to Apt sources
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
      $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
      sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
    
    sudo apt-get update -y

    # Install Docker Engine and Plugins
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi

# Enable and start Docker service
log_info "Ensuring Docker service is enabled and started..."
sudo systemctl enable docker
sudo systemctl start docker

# Add user to docker group
if ! groups $USER | grep -q '\bdocker\b'; then
    log_info "Adding user ($USER) to the 'docker' group..."
    sudo usermod -aG docker $USER
    log_info "Note: You may need to log out and log back in for group changes to take effect."
else
    log_info "User ($USER) is already in the 'docker' group."
fi

# 2. Nginx Installation
if command_exists nginx; then
    log_info "Nginx is already installed. Skipping installation."
else
    log_info "Installing Nginx..."
    sudo apt-get install -y nginx
fi

# Enable and start Nginx service
log_info "Ensuring Nginx service is enabled and started..."
sudo systemctl enable nginx
sudo systemctl start nginx

# 3. Certbot Installation
if command_exists certbot; then
    log_info "Certbot is already installed. Skipping installation."
else
    log_info "Installing Certbot and python3-certbot-nginx..."
    sudo apt-get install -y certbot python3-certbot-nginx
fi

# Verification
log_info "Verifying installations..."

# Print installed versions
echo ""
echo "===== INSTALLATION COMPLETE ====="
echo ""

if command_exists docker; then
    DOCKER_VERSION=$(docker --version)
    echo "Docker: $DOCKER_VERSION"
else
    echo "Docker: NOT INSTALLED"
fi

if command_exists docker && docker compose version >/dev/null 2>&1; then
    COMPOSE_VERSION=$(docker compose version)
    echo "Docker Compose: $COMPOSE_VERSION"
else
    echo "Docker Compose: NOT INSTALLED"
fi

if command_exists nginx; then
    NGINX_VERSION=$(nginx -v 2>&1)
    echo "Nginx: $NGINX_VERSION"
else
    echo "Nginx: NOT INSTALLED"
fi

if command_exists certbot; then
    CERTBOT_VERSION=$(certbot --version)
    echo "Certbot: $CERTBOT_VERSION"
else
    echo "Certbot: NOT INSTALLED"
fi

echo ""
log_info "Server preparation finished successfully!"

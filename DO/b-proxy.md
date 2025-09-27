Normal

```
# Load Balanced Instance Group Example
# Direct load balancing with HTTP(S) Load Balancer to instance groups

terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 4.0"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

# Variables
variable "project_id" {
  description = "GCP Project ID"
  type        = string
}

variable "region" {
  description = "GCP Region"
  type        = string
  default     = "us-central1"
}

variable "zone" {
  description = "GCP Zone"
  type        = string
  default     = "us-central1-a"
}

# VPC Network
resource "google_compute_network" "lb_network" {
  name                    = "lb-network"
  auto_create_subnetworks = false
}

# Subnet
resource "google_compute_subnetwork" "lb_subnet" {
  name          = "lb-subnet"
  ip_cidr_range = "10.0.0.0/24"
  region        = var.region
  network       = google_compute_network.lb_network.id
}

# Firewall rule to allow health checks
resource "google_compute_firewall" "allow_health_check" {
  name    = "allow-health-check"
  network = google_compute_network.lb_network.name

  allow {
    protocol = "tcp"
    ports    = ["80", "8080"]
  }

  # Google Cloud health check IP ranges
  source_ranges = ["130.211.0.0/22", "35.191.0.0/16"]
  target_tags   = ["web-server"]
}

# Firewall rule to allow HTTP traffic from load balancer
resource "google_compute_firewall" "allow_http" {
  name    = "allow-http"
  network = google_compute_network.lb_network.name

  allow {
    protocol = "tcp"
    ports    = ["80"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["web-server"]
}

# Instance template for web servers
resource "google_compute_instance_template" "web_template" {
  name_prefix  = "web-server-template-"
  machine_type = "e2-medium"

  disk {
    source_image = "debian-cloud/debian-11"
    auto_delete  = true
    boot         = true
    disk_size_gb = 20
  }

  network_interface {
    network    = google_compute_network.lb_network.id
    subnetwork = google_compute_subnetwork.lb_subnet.id
    access_config {
      # Ephemeral external IP
    }
  }

  metadata = {
    startup-script = <<-EOF
      #!/bin/bash
      apt-get update
      apt-get install -y apache2
      
      # Configure Apache
      systemctl enable apache2
      systemctl start apache2
      
      # Create a simple web page with instance info
      cat > /var/www/html/index.html << 'HTML'
      <!DOCTYPE html>
      <html>
      <head>
          <title>Load Balanced Web Server</title>
          <style>
              body { font-family: Arial, sans-serif; margin: 40px; background: #f0f0f0; }
              .container { background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
              .server-info { background: #e3f2fd; padding: 15px; border-radius: 5px; margin: 20px 0; }
              .status { color: #2e7d32; font-weight: bold; }
          </style>
      </head>
      <body>
          <div class="container">
              <h1>🚀 Load Balanced Web Server</h1>
              <div class="server-info">
                  <h3>Server Information:</h3>
                  <p><strong>Instance:</strong> $(hostname)</p>
                  <p><strong>Zone:</strong> $(curl -s -H "Metadata-Flavor: Google" http://metadata.google.internal/computeMetadata/v1/instance/zone | cut -d'/' -f4)</p>
                  <p><strong>Internal IP:</strong> $(hostname -I | cut -d' ' -f1)</p>
                  <p><strong>Timestamp:</strong> $(date)</p>
              </div>
              <p class="status">✅ Server is running and healthy!</p>
              <p>This server is part of a managed instance group behind a Google Cloud HTTP(S) Load Balancer.</p>
          </div>
      </body>
      </html>
HTML
      
      # Create health check endpoint
      cat > /var/www/html/health << 'HEALTH'
      OK
HEALTH
      
      # Restart Apache to apply changes
      systemctl restart apache2
      
      # Configure Apache for health checks
      a2enmod rewrite
      systemctl restart apache2
    EOF
  }

  tags = ["web-server"]

  lifecycle {
    create_before_destroy = true
  }
}

# Managed Instance Group
resource "google_compute_instance_group_manager" "web_igm" {
  name               = "web-instance-group"
  zone               = var.zone
  base_instance_name = "web-server"
  target_size        = 3

  version {
    instance_template = google_compute_instance_template.web_template.id
  }

  named_port {
    name = "http"
    port = 80
  }

  # Auto-healing policy
  auto_healing_policies {
    health_check      = google_compute_health_check.autohealing.id
    initial_delay_sec = 60
  }
}

# Autoscaler for the instance group
resource "google_compute_autoscaler" "web_autoscaler" {
  name   = "web-autoscaler"
  zone   = var.zone
  target = google_compute_instance_group_manager.web_igm.id

  autoscaling_policy {
    max_replicas    = 10
    min_replicas    = 2
    cooldown_period = 60

    cpu_utilization {
      target = 0.7
    }

    load_balancing_utilization {
      target = 0.8
    }
  }
}

# Health check for load balancer
resource "google_compute_health_check" "web_health_check" {
  name               = "web-health-check"
  check_interval_sec = 10
  timeout_sec        = 5
  healthy_threshold   = 2
  unhealthy_threshold = 3

  http_health_check {
    port               = "80"
    request_path       = "/health"
    response           = "OK"
  }
}

# Health check for auto-healing (more lenient)
resource "google_compute_health_check" "autohealing" {
  name               = "web-autohealing-health-check"
  check_interval_sec = 30
  timeout_sec        = 10
  healthy_threshold   = 2
  unhealthy_threshold = 5

  http_health_check {
    port         = "80"
    request_path = "/health"
  }
}

# Backend service
resource "google_compute_backend_service" "web_backend" {
  name                  = "web-backend-service"
  protocol              = "HTTP"
  timeout_sec           = 30
  enable_cdn           = false
  load_balancing_scheme = "EXTERNAL"

  backend {
    group           = google_compute_instance_group_manager.web_igm.instance_group
    balancing_mode  = "UTILIZATION"
    capacity_scaler = 1.0
    max_utilization = 0.8
  }

  health_checks = [google_compute_health_check.web_health_check.id]

  log_config {
    enable      = true
    sample_rate = 1.0
  }
}

# URL map
resource "google_compute_url_map" "web_url_map" {
  name            = "web-url-map"
  default_service = google_compute_backend_service.web_backend.id

  # Path-based routing example
  path_matcher {
    name            = "allpaths"
    default_service = google_compute_backend_service.web_backend.id

    path_rule {
      paths   = ["/api/*"]
      service = google_compute_backend_service.web_backend.id
    }
  }

  host_rule {
    hosts        = ["*"]
    path_matcher = "allpaths"
  }
}

# HTTP target proxy
resource "google_compute_target_http_proxy" "web_proxy" {
  name    = "web-http-proxy"
  url_map = google_compute_url_map.web_url_map.id
}

# Global forwarding rule (load balancer frontend)
resource "google_compute_global_forwarding_rule" "web_forwarding_rule" {
  name       = "web-forwarding-rule"
  target     = google_compute_target_http_proxy.web_proxy.id
  port_range = "80"
}

# Outputs
output "load_balancer_ip" {
  description = "IP address of the load balancer"
  value       = google_compute_global_forwarding_rule.web_forwarding_rule.ip_address
}

output "instance_group_url" {
  description = "URL of the managed instance group"
  value       = google_compute_instance_group_manager.web_igm.instance_group
}

output "backend_service_name" {
  description = "Name of the backend service"
  value       = google_compute_backend_service.web_backend.name
}

# Example usage:
# terraform init
# terraform plan -var="project_id=your-project-id"
# terraform apply -var="project_id=your-project-id"
# 
# Test the load balancer:
# curl http://$(terraform output -raw load_balancer_ip)
# 
# Monitor the instances:
# gcloud compute instances list --filter="name~'web-server.*'"
```

#### 2nd example with Proxy


```
# Instance Group Behind Proxy Example
# Instance group with proxy servers (Nginx/HAProxy) handling client connections

terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 4.0"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

# Variables
variable "project_id" {
  description = "GCP Project ID"
  type        = string
}

variable "region" {
  description = "GCP Region"
  type        = string
  default     = "us-central1"
}

variable "zone" {
  description = "GCP Zone"
  type        = string
  default     = "us-central1-a"
}

# VPC Network
resource "google_compute_network" "proxy_network" {
  name                    = "proxy-network"
  auto_create_subnetworks = false
}

# Frontend subnet for proxy servers
resource "google_compute_subnetwork" "frontend_subnet" {
  name          = "frontend-subnet"
  ip_cidr_range = "10.1.0.0/24"
  region        = var.region
  network       = google_compute_network.proxy_network.id
}

# Backend subnet for application servers
resource "google_compute_subnetwork" "backend_subnet" {
  name          = "backend-subnet"
  ip_cidr_range = "10.1.1.0/24"
  region        = var.region
  network       = google_compute_network.proxy_network.id
}

# Firewall rules for proxy tier
resource "google_compute_firewall" "allow_proxy_http" {
  name    = "allow-proxy-http"
  network = google_compute_network.proxy_network.name

  allow {
    protocol = "tcp"
    ports    = ["80", "443"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["proxy-server"]
}

# Firewall rules for health checks
resource "google_compute_firewall" "allow_health_check_proxy" {
  name    = "allow-health-check-proxy"
  network = google_compute_network.proxy_network.name

  allow {
    protocol = "tcp"
    ports    = ["80", "8080"]
  }

  source_ranges = ["130.211.0.0/22", "35.191.0.0/16"]
  target_tags   = ["proxy-server", "app-server"]
}

# Internal firewall rule for proxy to backend communication
resource "google_compute_firewall" "allow_internal_proxy_to_backend" {
  name    = "allow-internal-proxy-to-backend"
  network = google_compute_network.proxy_network.name

  allow {
    protocol = "tcp"
    ports    = ["8080", "9000"]
  }

  source_tags = ["proxy-server"]
  target_tags = ["app-server"]
}

# Backend Application Instance Template
resource "google_compute_instance_template" "app_template" {
  name_prefix  = "app-server-template-"
  machine_type = "e2-medium"

  disk {
    source_image = "debian-cloud/debian-11"
    auto_delete  = true
    boot         = true
    disk_size_gb = 20
  }

  network_interface {
    network    = google_compute_network.proxy_network.id
    subnetwork = google_compute_subnetwork.backend_subnet.id
    # No external IP - private backend servers
  }

  metadata = {
    startup-script = <<-EOF
      #!/bin/bash
      apt-get update
      apt-get install -y python3 python3-pip

      # Create a simple Python Flask application
      cat > /opt/app.py << 'PYTHON'
import os
import socket
from datetime import datetime
from flask import Flask, jsonify, request

app = Flask(__name__)

@app.route('/')
def home():
    return f'''
    <!DOCTYPE html>
    <html>
    <head>
        <title>Backend Application Server</title>
        <style>
            body {{ font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }}
            .container {{ background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }}
            .server-info {{ background: #fff3e0; padding: 15px; border-radius: 5px; margin: 20px 0; }}
            .status {{ color: #e65100; font-weight: bold; }}
            .proxy-info {{ background: #e8f5e8; padding: 15px; border-radius: 5px; margin: 20px 0; }}
        </style>
    </head>
    <body>
        <div class="container">
            <h1>🔧 Backend Application Server</h1>
            <div class="server-info">
                <h3>Backend Server Info:</h3>
                <p><strong>Hostname:</strong> {socket.gethostname()}</p>
                <p><strong>Internal IP:</strong> {socket.getfqdn()}</p>
                <p><strong>Process ID:</strong> {os.getpid()}</p>
                <p><strong>Timestamp:</strong> {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}</p>
            </div>
            <div class="proxy-info">
                <h3>Request Headers (from Proxy):</h3>
                <p><strong>X-Forwarded-For:</strong> {request.headers.get('X-Forwarded-For', 'Not set')}</p>
                <p><strong>X-Real-IP:</strong> {request.headers.get('X-Real-IP', 'Not set')}</p>
                <p><strong>User-Agent:</strong> {request.headers.get('User-Agent', 'Not set')}</p>
            </div>
            <p class="status">✅ Backend application is running behind proxy!</p>
        </div>
    </body>
    </html>
    '''

@app.route('/health')
def health():
    return 'OK', 200

@app.route('/api/status')
def api_status():
    return jsonify({
        'status': 'healthy',
        'hostname': socket.gethostname(),
        'timestamp': datetime.now().isoformat(),
        'headers': dict(request.headers)
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8080, debug=False)
PYTHON

      # Install Flask and run the application
      pip3 install flask
      
      # Create systemd service for the app
      cat > /etc/systemd/system/flask-app.service << 'SERVICE'
[Unit]
Description=Flask Application
After=network.target

[Service]
Type=simple
User=www-data
WorkingDirectory=/opt
Environment=PATH=/usr/bin
ExecStart=/usr/bin/python3 /opt/app.py
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
SERVICE

      # Start the application service
      systemctl daemon-reload
      systemctl enable flask-app
      systemctl start flask-app
    EOF
  }

  tags = ["app-server"]

  lifecycle {
    create_before_destroy = true
  }
}

# Backend Application Instance Group
resource "google_compute_instance_group_manager" "app_igm" {
  name               = "app-instance-group"
  zone               = var.zone
  base_instance_name = "app-server"
  target_size        = 3

  version {
    instance_template = google_compute_instance_template.app_template.id
  }

  named_port {
    name = "http"
    port = 8080
  }

  auto_healing_policies {
    health_check      = google_compute_health_check.app_autohealing.id
    initial_delay_sec = 120
  }
}

# Autoscaler for backend applications
resource "google_compute_autoscaler" "app_autoscaler" {
  name   = "app-autoscaler"
  zone   = var.zone
  target = google_compute_instance_group_manager.app_igm.id

  autoscaling_policy {
    max_replicas    = 8
    min_replicas    = 2
    cooldown_period = 60

    cpu_utilization {
      target = 0.6
    }
  }
}

# Proxy Instance Template (Nginx reverse proxy)
resource "google_compute_instance_template" "proxy_template" {
  name_prefix  = "proxy-server-template-"
  machine_type = "e2-standard-2"

  disk {
    source_image = "debian-cloud/debian-11"
    auto_delete  = true
    boot         = true
    disk_size_gb = 20
  }

  network_interface {
    network    = google_compute_network.proxy_network.id
    subnetwork = google_compute_subnetwork.frontend_subnet.id
    access_config {
      # External IP for proxy servers
    }
  }

  metadata = {
    startup-script = <<-EOF
      #!/bin/bash
      apt-get update
      apt-get install -y nginx

      # Configure Nginx as a reverse proxy
      cat > /etc/nginx/sites-available/default << 'NGINX'
upstream backend_servers {
    # Backend server IPs will be dynamically updated
    # For demo purposes, using a simple configuration
    server 10.1.1.2:8080 max_fails=3 fail_timeout=30s;
    server 10.1.1.3:8080 max_fails=3 fail_timeout=30s;
    server 10.1.1.4:8080 max_fails=3 fail_timeout=30s;
    
    # Enable session persistence based on IP
    ip_hash;
}

server {
    listen 80 default_server;
    server_name _;
    
    # Proxy configuration
    location / {
        proxy_pass http://backend_servers;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Proxy timeouts
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
        
        # Buffer settings
        proxy_buffering on;
        proxy_buffer_size 128k;
        proxy_buffers 4 256k;
        proxy_busy_buffers_size 256k;
    }
    
    # Health check endpoint for load balancer
    location /proxy-health {
        access_log off;
        return 200 "Proxy OK\n";
        add_header Content-Type text/plain;
    }
    
    # Nginx status for monitoring
    location /nginx_status {
        stub_status on;
        access_log off;
        allow 127.0.0.1;
        allow 10.1.0.0/24;
        deny all;
    }
    
    # Static files served directly by proxy
    location /static/ {
        alias /var/www/static/;
        expires 1d;
        add_header Cache-Control "public, immutable";
    }
}

# Configuration for HTTPS (if needed)
server {
    listen 443 ssl http2;
    server_name _;
    
    # SSL configuration would go here
    # ssl_certificate /path/to/cert.pem;
    # ssl_certificate_key /path/to/key.pem;
    
    location / {
        proxy_pass http://backend_servers;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }
}
NGINX

      # Create static directory
      mkdir -p /var/www/static
      cat > /var/www/static/proxy-info.html << 'HTML'
<!DOCTYPE html>
<html>
<head><title>Proxy Static Content</title></head>
<body>
    <h1>🔄 Static Content Served by Proxy</h1>
    <p>This content is served directly by the Nginx proxy without hitting backend servers.</p>
</body>
</html>
HTML

      # Configure Nginx performance settings
      cat > /etc/nginx/nginx.conf << 'NGINXCONF'
user www-data;
worker_processes auto;
worker_rlimit_nofile 65535;
pid /run/nginx.pid;

events {
    worker_connections 4096;
    use epoll;
    multi_accept on;
}

http {
    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;
    server_tokens off;
    
    include /etc/nginx/mime.types;
    default_type application/octet-stream;
    
    # Logging
    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                   '$status $body_bytes_sent "$http_referer" '
                   '"$http_user_agent" "$http_x_forwarded_for" '
                   'upstream_addr=$upstream_addr '
                   'upstream_response_time=$upstream_response_time';
    
    access_log /var/log/nginx/access.log main;
    error_log /var/log/nginx/error.log;
    
    # Gzip compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_comp_level 6;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;
    
    include /etc/nginx/conf.d/*.conf;
    include /etc/nginx/sites-enabled/*;
}
NGINXCONF

      # Start and enable Nginx
      systemctl enable nginx
      systemctl start nginx
      
      # Configure log rotation
      cat > /etc/logrotate.d/nginx << 'LOGROTATE'
/var/log/nginx/*.log {
    daily
    missingok
    rotate 52
    compress
    delaycompress
    notifempty
    create 644 www-data adm
    sharedscripts
    prerotate
        if [ -d /etc/logrotate.d/httpd-prerotate ]; then \
            run-parts /etc/logrotate.d/httpd-prerotate; \
        fi
    endscript
    postrotate
        invoke-rc.d nginx rotate >/dev/null 2>&1
    endscript
}
LOGROTATE
    EOF
  }

  tags = ["proxy-server"]

  lifecycle {
    create_before_destroy = true
  }
}

# Proxy Instance Group
resource "google_compute_instance_group_manager" "proxy_igm" {
  name               = "proxy-instance-group"
  zone               = var.zone
  base_instance_name = "proxy-server"
  target_size        = 2

  version {
    instance_template = google_compute_instance_template.proxy_template.id
  }

  named_port {
    name = "http"
    port = 80
  }

  named_port {
    name = "https"
    port = 443
  }

  auto_healing_policies {
    health_check      = google_compute_health_check.proxy_autohealing.id
    initial_delay_sec = 60
  }
}

# Autoscaler for proxy servers
resource "google_compute_autoscaler" "proxy_autoscaler" {
  name   = "proxy-autoscaler"
  zone   = var.zone
  target = google_compute_instance_group_manager.proxy_igm.id

  autoscaling_policy {
    max_replicas    = 6
    min_replicas    = 2
    cooldown_period = 60

    cpu_utilization {
      target = 0.7
    }
  }
}

# Health checks
resource "google_compute_health_check" "proxy_health_check" {
  name               = "proxy-health-check"
  check_interval_sec = 10
  timeout_sec        = 5
  healthy_threshold   = 2
  unhealthy_threshold = 3

  http_health_check {
    port         = "80"
    request_path = "/proxy-health"
  }
}

resource "google_compute_health_check" "proxy_autohealing" {
  name               = "proxy-autohealing-health-check"
  check_interval_sec = 30
  timeout_sec        = 10
  healthy_threshold   = 2
  unhealthy_threshold = 5

  http_health_check {
    port         = "80"
    request_path = "/proxy-health"
  }
}

resource "google_compute_health_check" "app_autohealing" {
  name               = "app-autohealing-health-check"
  check_interval_sec = 30
  timeout_sec        = 10
  healthy_threshold   = 2
  unhealthy_threshold = 5

  http_health_check {
    port         = "8080"
    request_path = "/health"
  }
}

# Backend service for proxy servers
resource "google_compute_backend_service" "proxy_backend" {
  name                  = "proxy-backend-service"
  protocol              = "HTTP"
  timeout_sec           = 30
  enable_cdn           = true
  load_balancing_scheme = "EXTERNAL"

  backend {
    group           = google_compute_instance_group_manager.proxy_igm.instance_group
    balancing_mode  = "UTILIZATION"
    capacity_scaler = 1.0
    max_utilization = 0.8
  }

  health_checks = [google_compute_health_check.proxy_health_check.id]

  # CDN configuration for static content served by proxy
  cdn_policy {
    cache_mode                   = "CACHE_ALL_STATIC"
    default_ttl                 = 3600
    max_ttl                     = 86400
    negative_caching            = true
    
    cache_key_policy {
      include_host         = true
      include_protocol     = true
      include_query_string = false
    }
  }

  log_config {
    enable      = true
    sample_rate = 1.0
  }
}

# URL map with advanced routing
resource "google_compute_url_map" "proxy_url_map" {
  name            = "proxy-url-map"
  default_service = google_compute_backend_service.proxy_backend.id

  # Path-based routing
  path_matcher {
    name            = "proxy-paths"
    default_service = google_compute_backend_service.proxy_backend.id

    # Static content served with longer cache
    path_rule {
      paths   = ["/static/*"]
      service = google_compute_backend_service.proxy_backend.id
    }

    # API routes
    path_rule {
      paths   = ["/api/*"]
      service = google_compute_backend_service.proxy_backend.id
    }
  }

  host_rule {
    hosts        = ["*"]
    path_matcher = "proxy-paths"
  }
}

# HTTP target proxy
resource "google_compute_target_http_proxy" "proxy_target_proxy" {
  name    = "proxy-http-proxy"
  url_map = google_compute_url_map.proxy_url_map.id
}

# Global forwarding rule
resource "google_compute_global_forwarding_rule" "proxy_forwarding_rule" {
  name       = "proxy-forwarding-rule"
  target     = google_compute_target_http_proxy.proxy_target_proxy.id
  port_range = "80"
}

# Internal load balancer for backend communication (optional enhancement)
resource "google_compute_region_backend_service" "internal_backend" {
  name                  = "internal-backend-service"
  region                = var.region
  protocol              = "HTTP"
  load_balancing_scheme = "INTERNAL"

  backend {
    group = google_compute_instance_group_manager.app_igm.instance_group
  }

  health_checks = [google_compute_health_check.app_autohealing.id]
}

# Internal forwarding rule for backend service discovery
resource "google_compute_forwarding_rule" "internal_forwarding_rule" {
  name                  = "internal-forwarding-rule"
  region                = var.region
  load_balancing_scheme = "INTERNAL"
  backend_service       = google_compute_region_backend_service.internal_backend.id
  all_ports             = true
  network               = google_compute_network.proxy_network.id
  subnetwork            = google_compute_subnetwork.backend_subnet.id
}

# Cloud NAT for backend servers to access internet (for updates, etc.)
resource "google_compute_router" "nat_router" {
  name    = "nat-router"
  region  = var.region
  network = google_compute_network.proxy_network.id
}

resource "google_compute_router_nat" "nat_gateway" {
  name                               = "nat-gateway"
  router                            = google_compute_router.nat_router.name
  region                            = google_compute_router.nat_router.region
  nat_ip_allocate_option            = "AUTO_ONLY"
  source_subnetwork_ip_ranges_to_nat = "LIST_OF_SUBNETWORKS"

  subnetwork {
    name                    = google_compute_subnetwork.backend_subnet.id
    source_ip_ranges_to_nat = ["ALL_IP_RANGES"]
  }
}

# Outputs
output "proxy_load_balancer_ip" {
  description = "IP address of the proxy load balancer"
  value       = google_compute_global_forwarding_rule.proxy_forwarding_rule.ip_address
}

output "proxy_instance_group" {
  description = "URL of the proxy instance group"
  value       = google_compute_instance_group_manager.proxy_igm.instance_group
}

output "backend_instance_group" {
  description = "URL of the backend instance group"
  value       = google_compute_instance_group_manager.app_igm.instance_group
}

output "internal_load_balancer_ip" {
  description = "IP address of the internal load balancer for backends"
  value       = google_compute_forwarding_rule.internal_forwarding_rule.ip_address
}

# Example usage commands:
# 
# Deploy the infrastructure:
# terraform init
# terraform plan -var="project_id=your-project-id"
# terraform apply -var="project_id=your-project-id"
# 
# Test the proxy setup:
# curl http://$(terraform output -raw proxy_load_balancer_ip)
# curl http://$(terraform output -raw proxy_load_balancer_ip)/static/proxy-info.html
# curl http://$(terraform output -raw proxy_load_balancer_ip)/api/status
# 
# Monitor the setup:
# gcloud compute instances list --filter="name~'proxy-server.*'"
# gcloud compute instances list --filter="name~'app-server.*'"
# 
# Check proxy logs:
# gcloud compute ssh proxy-server-xxxx --zone=us-central1-a --command="sudo tail -f /var/log/nginx/access.log"
# 
# Architecture Summary:
# Internet → GCP Load Balancer → Proxy Instances (Nginx) → Backend App Instances (Flask)
#           ↓                    ↓                         ↓
#        Port 80/443          Port 80/443              Port 8080
#                             Frontend Subnet          Backend Subnet
#                             (10.1.0.0/24)           (10.1.1.0/24)
# 
# Key Features:
# - Multi-tier architecture with separation of concerns
# - Nginx reverse proxy for SSL termination, caching, and load balancing
# - Backend applications in private subnet (no external IPs)
# - Auto-scaling for both proxy and backend tiers
# - Internal load balancer for backend service discovery
# - CDN integration for static content
# - Health checks at both tiers
# - Cloud NAT for backend internet access

```
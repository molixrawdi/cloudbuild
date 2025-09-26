Principal DevOps Engineer - Load Balancer Performance Scenario Test
Scenario Overview
You are the Principal DevOps Engineer for a high-traffic e-commerce platform experiencing rapid growth. The application currently serves 50,000 concurrent users during peak hours, but marketing forecasts indicate traffic will triple during the upcoming holiday season. The current infrastructure is showing signs of strain with increased latency and occasional timeouts.
Current Infrastructure State
The existing setup consists of:

A single Google Cloud HTTP(S) Load Balancer
6 Compute Engine instances in a single zone (us-central1-a)
Basic health checks with 30-second intervals
No connection draining configured
Static IP allocation
Basic logging enabled

Standard Terraform Configuration
```


# Provider configuration
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

# VPC Network
resource "google_compute_network" "vpc_network" {
  name                    = "ecommerce-vpc"
  auto_create_subnetworks = false
}

# Subnet
resource "google_compute_subnetwork" "subnet" {
  name          = "ecommerce-subnet"
  ip_cidr_range = "10.0.0.0/24"
  region        = var.region
  network       = google_compute_network.vpc_network.id
}

# Instance Template
resource "google_compute_instance_template" "web_template" {
  name_prefix  = "ecommerce-template-"
  machine_type = "e2-standard-2"

  disk {
    source_image = "debian-cloud/debian-11"
    auto_delete  = true
    boot         = true
  }

  network_interface {
    network    = google_compute_network.vpc_network.id
    subnetwork = google_compute_subnetwork.subnet.id
    access_config {
      // Ephemeral IP
    }
  }

  metadata_startup_script = file("startup-script.sh")

  lifecycle {
    create_before_destroy = true
  }
}

# Instance Group Manager
resource "google_compute_instance_group_manager" "web_igm" {
  name               = "ecommerce-igm"
  zone               = "${var.region}-a"
  base_instance_name = "ecommerce-web"
  target_size        = 6

  version {
    instance_template = google_compute_instance_template.web_template.id
  }

  named_port {
    name = "http"
    port = 80
  }
}

# Health Check
resource "google_compute_health_check" "web_health_check" {
  name = "ecommerce-health-check"

  http_health_check {
    port = "80"
    request_path = "/health"
  }

  timeout_sec         = 5
  check_interval_sec  = 30
  healthy_threshold   = 2
  unhealthy_threshold = 3
}

# Backend Service
resource "google_compute_backend_service" "web_backend" {
  name                  = "ecommerce-backend"
  protocol              = "HTTP"
  timeout_sec           = 30
  enable_cdn            = false
  load_balancing_scheme = "EXTERNAL"

  backend {
    group = google_compute_instance_group_manager.web_igm.instance_group
  }

  health_checks = [google_compute_health_check.web_health_check.id]
}

# URL Map
resource "google_compute_url_map" "web_url_map" {
  name            = "ecommerce-url-map"
  default_service = google_compute_backend_service.web_backend.id
}

# HTTP Proxy
resource "google_compute_target_http_proxy" "web_proxy" {
  name    = "ecommerce-http-proxy"
  url_map = google_compute_url_map.web_url_map.id
}

# Global Forwarding Rule
resource "google_compute_global_forwarding_rule" "web_forwarding_rule" {
  name       = "ecommerce-forwarding-rule"
  target     = google_compute_target_http_proxy.web_proxy.id
  port_range = "80"
}

# Firewall Rules
resource "google_compute_firewall" "allow_http" {
  name    = "allow-http"
  network = google_compute_network.vpc_network.name

  allow {
    protocol = "tcp"
    ports    = ["80"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["web-server"]
}

resource "google_compute_firewall" "allow_health_check" {
  name    = "allow-health-check"
  network = google_compute_network.vpc_network.name

  allow {
    protocol = "tcp"
    ports    = ["80"]
  }

  source_ranges = ["130.211.0.0/22", "35.191.0.0/16"]
  target_tags   = ["web-server"]
}

```

Advanced example solutions:

Connection draining and rolling updates for zero-downtime deployments
Advanced Load Balancing

Session affinity for shopping cart persistence
Intelligent autoscaling based on CPU, load balancer utilization, and network metrics
Enhanced health checks with 10-second intervals for faster failure detection
Separate backend services for web and API traffic with different configurations

Security & Performance Optimization

HTTPS with managed SSL certificates and HTTP-to-HTTPS redirects
Cloud CDN integration for static assets and caching policies
DDoS protection with rate limiting and adaptive protection
IAP (Identity-Aware Proxy) for additional security layer
Private instances using NAT gateways (no external IPs)

Scalability Features

Auto-scaling from 3 to 50 instances per region with intelligent scaling policies
Scale-in controls to prevent rapid downscaling
Separate static asset serving via Cloud Storage backend
Advanced URL routing for different content types

Operational Excellence

Comprehensive logging with configurable sample rates
Uptime monitoring with automated health checks
Auto-healing policies for automatic instance replacement
Service accounts with least-privilege access

Key Enhancements for the Scenario

Capacity Planning: The configuration can handle 150K+ concurrent users through:

Up to 100 instances across two regions (50 each)
CPU-optimized machines with better performance per instance
CDN offloading for static content


Performance Improvements:

Sub-200ms response times through CDN, optimized health checks, and efficient load balancing
Session affinity maintains shopping cart state
Separate API backend with longer timeouts for complex operations


Cost Optimization:

Autoscaling prevents over-provisioning during low traffic
CDN reduces compute load for static assets
Private instances reduce NAT gateway costs


Monitoring Strategy:

Multiple health check types (load balancing vs. auto-healing)
Comprehensive logging with different sample rates
Uptime monitoring for proactive alerting


This enhanced configuration transforms a basic single-zone setup into a production-ready, globally distributed system capable of handling Black Friday-level traffic while maintaining performance, security, and cost-effectiveness. The Terraform code demonstrates advanced GCP networking concepts, infrastructure as code best practices, and operational readiness for enterprise-scale deployments.RetryClaude can make mistakes. Please double-check responses.


Q Scenario:

Test Scenarios
1. Performance Analysis Task
Review the current configuration and identify at least 5 performance bottlenecks. Document your findings with specific reasons why each element impacts performance.
2. Load Testing Setup
Design a load testing strategy to simulate the expected 150,000 concurrent users. Specify:

Tools you would use
Test scenarios to run
Metrics to monitor
Success criteria

3. Scaling Strategy
The application shows these performance characteristics:

Average response time: 250ms at current load
CPU utilization: 70% average, 90% peak
Memory utilization: 60% average
Network throughput: 500 Mbps peak

Design an auto-scaling strategy that can handle the 3x traffic increase while maintaining sub-200ms response times.
Expected Deliverables

Problem Analysis Report (15 minutes)

Identify performance bottlenecks
Prioritize issues by impact
Propose initial solutions


Enhanced Terraform Configuration (30 minutes)

Implement your recommended improvements
Include detailed comments explaining changes
Ensure high availability and performance


Operational Plan (15 minutes)

Deployment strategy for the changes
Monitoring and alerting setup
Rollback procedures



Evaluation Criteria

Technical Knowledge: Understanding of GCP load balancing, networking, and performance optimization
Problem-Solving: Ability to identify root causes and design appropriate solutions
Terraform Proficiency: Clean, modular, and maintainable infrastructure as code
Operational Awareness: Consideration of deployment risks, monitoring, and maintenance
Scalability Design: Forward-thinking approach to handle future growth
Communication: Clear documentation and explanation of design decisions

Additional Context

Budget constraints require cost-effective solutions
The application is stateless but has sticky session requirements for shopping carts
Compliance requirements mandate traffic logging and monitoring
The team has limited experience with advanced GCP networking features
Peak traffic occurs during 6-hour windows (Black Friday, Cyber Monday)

Time Allocation

Total time: 60 minutes
Analysis and planning: 15 minutes
Implementation: 35 minutes
Documentation and presentation: 10 minutes

Enhanced example

```

# Enhanced Production-Ready Load Balancer Configuration
# Addresses performance bottlenecks and scalability requirements
# Key Improvements:
# - Multi-region deployment with auto-scaling
# - CDN integration and static asset optimization
# - Advanced security with DDoS protection
# - Session affinity for shopping cart persistence
# - Comprehensive monitoring and logging

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
  description = "Primary GCP Region"
  type        = string
  default     = "us-central1"
}

variable "secondary_region" {
  description = "Secondary GCP Region for multi-region deployment"
  type        = string
  default     = "us-east1"
}

variable "min_instances" {
  description = "Minimum number of instances"
  type        = number
  default     = 3
}

variable "max_instances" {
  description = "Maximum number of instances"
  type        = number
  default     = 50
}

# VPC Network with custom routing
resource "google_compute_network" "vpc_network" {
  name                    = "ecommerce-vpc"
  auto_create_subnetworks = false
  routing_mode           = "GLOBAL"
}

# Primary region subnet
resource "google_compute_subnetwork" "primary_subnet" {
  name          = "ecommerce-primary-subnet"
  ip_cidr_range = "10.0.1.0/24"
  region        = var.region
  network       = google_compute_network.vpc_network.id

  # Enable private Google access for better performance
  private_ip_google_access = true
}

# Secondary region subnet for multi-region deployment
resource "google_compute_subnetwork" "secondary_subnet" {
  name          = "ecommerce-secondary-subnet"
  ip_cidr_range = "10.0.2.0/24"
  region        = var.secondary_region
  network       = google_compute_network.vpc_network.id

  private_ip_google_access = true
}

# Cloud NAT for outbound internet access without external IPs
resource "google_compute_router" "router_primary" {
  name    = "ecommerce-router-primary"
  region  = var.region
  network = google_compute_network.vpc_network.id
}

resource "google_compute_router_nat" "nat_primary" {
  name                               = "ecommerce-nat-primary"
  router                            = google_compute_router.router_primary.name
  region                            = google_compute_router.router_primary.region
  nat_ip_allocate_option            = "AUTO_ONLY"
  source_subnetwork_ip_ranges_to_nat = "ALL_SUBNETWORKS_ALL_IP_RANGES"
}

resource "google_compute_router" "router_secondary" {
  name    = "ecommerce-router-secondary"
  region  = var.secondary_region
  network = google_compute_network.vpc_network.id
}

resource "google_compute_router_nat" "nat_secondary" {
  name                               = "ecommerce-nat-secondary"
  router                            = google_compute_router.router_secondary.name
  region                            = google_compute_router.router_secondary.region
  nat_ip_allocate_option            = "AUTO_ONLY"
  source_subnetwork_ip_ranges_to_nat = "ALL_SUBNETWORKS_ALL_IP_RANGES"
}

# Enhanced instance template with better performance specs
resource "google_compute_instance_template" "web_template_primary" {
  name_prefix  = "ecommerce-primary-template-"
  machine_type = "c2-standard-4"  # CPU-optimized for better performance

  disk {
    source_image = "debian-cloud/debian-11"
    auto_delete  = true
    boot         = true
    disk_size_gb = 50
    disk_type    = "pd-ssd"  # SSD for better I/O performance
  }

  network_interface {
    network    = google_compute_network.vpc_network.id
    subnetwork = google_compute_subnetwork.primary_subnet.id
    # No external IP for security - using NAT gateway
  }

  metadata = {
    startup-script = file("startup-script.sh")
    enable-oslogin = "TRUE"
  }

  tags = ["web-server", "primary-region"]

  service_account {
    email  = google_service_account.instance_sa.email
    scopes = ["cloud-platform"]
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "google_compute_instance_template" "web_template_secondary" {
  name_prefix  = "ecommerce-secondary-template-"
  machine_type = "c2-standard-4"

  disk {
    source_image = "debian-cloud/debian-11"
    auto_delete  = true
    boot         = true
    disk_size_gb = 50
    disk_type    = "pd-ssd"
  }

  network_interface {
    network    = google_compute_network.vpc_network.id
    subnetwork = google_compute_subnetwork.secondary_subnet.id
  }

  metadata = {
    startup-script = file("startup-script.sh")
    enable-oslogin = "TRUE"
  }

  tags = ["web-server", "secondary-region"]

  service_account {
    email  = google_service_account.instance_sa.email
    scopes = ["cloud-platform"]
  }

  lifecycle {
    create_before_destroy = true
  }
}

# Service account for instances
resource "google_service_account" "instance_sa" {
  account_id   = "ecommerce-instance-sa"
  display_name = "E-commerce Instance Service Account"
}

# Regional Instance Group Managers with autoscaling
resource "google_compute_region_instance_group_manager" "web_igm_primary" {
  name               = "ecommerce-igm-primary"
  region             = var.region
  base_instance_name = "ecommerce-web-primary"

  version {
    instance_template = google_compute_instance_template.web_template_primary.id
  }

  named_port {
    name = "http"
    port = 80
  }

  named_port {
    name = "https"
    port = 443
  }

  # Multi-zone distribution for high availability
  distribution_policy_zones = [
    "${var.region}-a",
    "${var.region}-b",
    "${var.region}-c"
  ]

  # Enable connection draining
  update_policy {
    type           = "PROACTIVE"
    minimal_action = "REPLACE"
    max_surge_fixed = 3
    max_unavailable_fixed = 1
  }

  auto_healing_policies {
    health_check      = google_compute_health_check.autohealing_health_check.id
    initial_delay_sec = 60
  }
}

resource "google_compute_region_instance_group_manager" "web_igm_secondary" {
  name               = "ecommerce-igm-secondary"
  region             = var.secondary_region
  base_instance_name = "ecommerce-web-secondary"

  version {
    instance_template = google_compute_instance_template.web_template_secondary.id
  }

  named_port {
    name = "http"
    port = 80
  }

  named_port {
    name = "https"
    port = 443
  }

  distribution_policy_zones = [
    "${var.secondary_region}-a",
    "${var.secondary_region}-b",
    "${var.secondary_region}-c"
  ]

  update_policy {
    type           = "PROACTIVE"
    minimal_action = "REPLACE"
    max_surge_fixed = 3
    max_unavailable_fixed = 1
  }

  auto_healing_policies {
    health_check      = google_compute_health_check.autohealing_health_check.id
    initial_delay_sec = 60
  }
}

# Autoscaler for primary region
resource "google_compute_region_autoscaler" "web_autoscaler_primary" {
  name   = "ecommerce-autoscaler-primary"
  region = var.region
  target = google_compute_region_instance_group_manager.web_igm_primary.id

  autoscaling_policy {
    max_replicas    = var.max_instances
    min_replicas    = var.min_instances
    cooldown_period = 60

    cpu_utilization {
      target = 0.6  # Scale up at 60% CPU to maintain performance
    }

    # Scale based on HTTP load balancer utilization
    load_balancing_utilization {
      target = 0.8
    }

    # Custom metric scaling for application-specific metrics
    metric {
      name   = "compute.googleapis.com/instance/network/received_bytes_count"
      type   = "GAUGE"
      target = 100
    }

    scale_in_control {
      max_scaled_in_replicas {
        fixed = 3
      }
      time_window_sec = 300
    }
  }
}

# Autoscaler for secondary region
resource "google_compute_region_autoscaler" "web_autoscaler_secondary" {
  name   = "ecommerce-autoscaler-secondary"
  region = var.secondary_region
  target = google_compute_region_instance_group_manager.web_igm_secondary.id

  autoscaling_policy {
    max_replicas    = var.max_instances
    min_replicas    = var.min_instances
    cooldown_period = 60

    cpu_utilization {
      target = 0.6
    }

    load_balancing_utilization {
      target = 0.8
    }

    scale_in_control {
      max_scaled_in_replicas {
        fixed = 3
      }
      time_window_sec = 300
    }
  }
}

# Enhanced health checks with faster intervals
resource "google_compute_health_check" "web_health_check" {
  name               = "ecommerce-health-check"
  check_interval_sec = 10  # Faster detection of unhealthy instances
  timeout_sec        = 5
  healthy_threshold   = 2
  unhealthy_threshold = 3

  http_health_check {
    port         = "80"
    request_path = "/health"
  }

  log_config {
    enable = true
  }
}

# Separate health check for autohealing (more aggressive)
resource "google_compute_health_check" "autohealing_health_check" {
  name               = "ecommerce-autohealing-health-check"
  check_interval_sec = 15
  timeout_sec        = 10
  healthy_threshold   = 2
  unhealthy_threshold = 5  # More tolerant to avoid unnecessary replacements

  http_health_check {
    port         = "80"
    request_path = "/health"
  }
}

# SSL Certificate for HTTPS
resource "google_compute_managed_ssl_certificate" "ssl_cert" {
  name = "ecommerce-ssl-cert"

  managed {
    domains = ["example.com", "www.example.com"]  # Replace with actual domains
  }
}

# Enhanced Backend Service with session affinity and CDN
resource "google_compute_backend_service" "web_backend" {
  name                            = "ecommerce-backend"
  protocol                       = "HTTP"
  timeout_sec                    = 30
  enable_cdn                     = true
  load_balancing_scheme          = "EXTERNAL_MANAGED"
  session_affinity              = "CLIENT_IP"  # For sticky sessions
  connection_draining_timeout_sec = 300

  # Primary region backend
  backend {
    group           = google_compute_region_instance_group_manager.web_igm_primary.instance_group
    balancing_mode  = "UTILIZATION"
    capacity_scaler = 1.0
    max_utilization = 0.8
  }

  # Secondary region backend
  backend {
    group           = google_compute_region_instance_group_manager.web_igm_secondary.instance_group
    balancing_mode  = "UTILIZATION"
    capacity_scaler = 1.0
    max_utilization = 0.8
  }

  health_checks = [google_compute_health_check.web_health_check.id]

  # CDN configuration
  cdn_policy {
    cache_mode                   = "CACHE_ALL_STATIC"
    default_ttl                 = 3600
    max_ttl                     = 86400
    negative_caching            = true
    serve_while_stale           = 86400
    
    cache_key_policy {
      include_host           = true
      include_protocol       = true
      include_query_string   = false
    }
  }

  # Security policy for DDoS protection
  security_policy = google_compute_security_policy.ddos_protection.id

  log_config {
    enable      = true
    sample_rate = 0.1  # Log 10% of requests for performance monitoring
  }

  iap {
    oauth2_client_id     = google_iap_client.project_client.client_id
    oauth2_client_secret = google_iap_client.project_client.secret
  }
}

# Security policy for DDoS protection
resource "google_compute_security_policy" "ddos_protection" {
  name        = "ecommerce-security-policy"
  description = "DDoS protection and rate limiting"

  rule {
    action   = "allow"
    priority = "1000"
    match {
      versioned_expr = "SRC_IPS_V1"
      config {
        src_ip_ranges = ["*"]
      }
    }
    description = "Default allow rule"
    
    rate_limit_options {
      conform_action = "allow"
      exceed_action  = "deny(429)"
      enforce_on_key = "IP"
      
      rate_limit_threshold {
        count        = 100
        interval_sec = 60
      }
    }
  }

  rule {
    action   = "deny(403)"
    priority = "2000"
    match {
      expr {
        expression = "origin.region_code == 'CN'"
      }
    }
    description = "Block traffic from specific regions if needed"
  }

  adaptive_protection_config {
    layer_7_ddos_defense_config {
      enable = true
    }
  }
}

# IAP OAuth client
resource "google_iap_client" "project_client" {
  display_name = "E-commerce Application"
  brand        = google_iap_brand.project_brand.name
}

resource "google_iap_brand" "project_brand" {
  support_email     = "support@example.com"
  application_title = "E-commerce Application"
}

# URL Map with advanced routing
resource "google_compute_url_map" "web_url_map" {
  name            = "ecommerce-url-map"
  default_service = google_compute_backend_service.web_backend.id

  # Route for API endpoints
  path_matcher {
    name            = "api-paths"
    default_service = google_compute_backend_service.web_backend.id

    path_rule {
      paths   = ["/api/*"]
      service = google_compute_backend_service.api_backend.id
    }

    path_rule {
      paths   = ["/static/*"]
      service = google_compute_backend_bucket.static_assets.id
    }
  }

  host_rule {
    hosts        = ["api.example.com"]
    path_matcher = "api-paths"
  }
}

# Separate backend for API with different configuration
resource "google_compute_backend_service" "api_backend" {
  name                            = "ecommerce-api-backend"
  protocol                       = "HTTP"
  timeout_sec                    = 60  # Longer timeout for API calls
  enable_cdn                     = false
  load_balancing_scheme          = "EXTERNAL_MANAGED"
  session_affinity              = "NONE"
  connection_draining_timeout_sec = 180

  backend {
    group           = google_compute_region_instance_group_manager.web_igm_primary.instance_group
    balancing_mode  = "RATE"
    max_rate        = 1000
  }

  backend {
    group           = google_compute_region_instance_group_manager.web_igm_secondary.instance_group
    balancing_mode  = "RATE"
    max_rate        = 1000
  }

  health_checks = [google_compute_health_check.api_health_check.id]

  log_config {
    enable      = true
    sample_rate = 1.0  # Full logging for API calls
  }
}

# API-specific health check
resource "google_compute_health_check" "api_health_check" {
  name               = "ecommerce-api-health-check"
  check_interval_sec = 15
  timeout_sec        = 10
  healthy_threshold   = 2
  unhealthy_threshold = 3

  http_health_check {
    port         = "80"
    request_path = "/api/health"
  }
}

# Static assets backend bucket
resource "google_storage_bucket" "static_assets" {
  name     = "${var.project_id}-static-assets"
  location = "US"

  website {
    main_page_suffix = "index.html"
    not_found_page   = "404.html"
  }

  cors {
    origin          = ["https://example.com", "https://www.example.com"]
    method          = ["GET", "HEAD"]
    response_header = ["*"]
    max_age_seconds = 3600
  }
}

# Backend bucket for static assets
resource "google_compute_backend_bucket" "static_assets" {
  name        = "ecommerce-static-assets"
  bucket_name = google_storage_bucket.static_assets.name
  enable_cdn  = true

  cdn_policy {
    default_ttl = 3600
    max_ttl     = 86400
  }
}

# HTTPS Proxy
resource "google_compute_target_https_proxy" "web_https_proxy" {
  name             = "ecommerce-https-proxy"
  url_map          = google_compute_url_map.web_url_map.id
  ssl_certificates = [google_compute_managed_ssl_certificate.ssl_cert.id]
}

# HTTP Proxy for redirects
resource "google_compute_target_http_proxy" "web_http_proxy" {
  name    = "ecommerce-http-proxy"
  url_map = google_compute_url_map.http_redirect.id
}

# HTTP to HTTPS redirect
resource "google_compute_url_map" "http_redirect" {
  name = "ecommerce-http-redirect"

  default_url_redirect {
    https_redirect         = true
    redirect_response_code = "MOVED_PERMANENTLY_DEFAULT"
    strip_query            = false
  }
}

# Global Forwarding Rules
resource "google_compute_global_forwarding_rule" "web_https_forwarding_rule" {
  name       = "ecommerce-https-forwarding-rule"
  target     = google_compute_target_https_proxy.web_https_proxy.id
  port_range = "443"
}

resource "google_compute_global_forwarding_rule" "web_http_forwarding_rule" {
  name       = "ecommerce-http-forwarding-rule"
  target     = google_compute_target_http_proxy.web_http_proxy.id
  port_range = "80"
}

# Enhanced Firewall Rules
resource "google_compute_firewall" "allow_lb_health_checks" {
  name    = "allow-lb-health-checks"
  network = google_compute_network.vpc_network.name

  allow {
    protocol = "tcp"
    ports    = ["80", "443"]
  }

  source_ranges = [
    "130.211.0.0/22",  # Google Load Balancer health check ranges
    "35.191.0.0/16"
  ]
  
  target_tags = ["web-server"]
}

resource "google_compute_firewall" "allow_internal" {
  name    = "allow-internal"
  network = google_compute_network.vpc_network.name

  allow {
    protocol = "tcp"
    ports    = ["0-65535"]
  }

  allow {
    protocol = "udp"
    ports    = ["0-65535"]
  }

  allow {
    protocol = "icmp"
  }

  source_ranges = ["10.0.0.0/16"]
}

resource "google_compute_firewall" "deny_all" {
  name    = "deny-all"
  network = google_compute_network.vpc_network.name

  deny {
    protocol = "all"
  }

  source_ranges = ["0.0.0.0/0"]
  priority      = 1000
}

# Monitoring and Alerting
resource "google_monitoring_uptime_check_config" "uptime_check" {
  display_name = "E-commerce Uptime Check"
  timeout      = "10s"
  period       = "60s"

  http_check {
    path         = "/health"
    port         = 443
    use_ssl      = true
    validate_ssl = true
  }

  monitored_resource {
    type = "uptime_url"
    labels = {
      project_id = var.project_id
      host       = "example.com"
    }
  }

  content_matchers {
    content = "OK"
    matcher = "CONTAINS_STRING"
  }
}

# Outputs for monitoring
output "load_balancer_ip" {
  description = "IP address of the load balancer"
  value       = google_compute_global_forwarding_rule.web_https_forwarding_rule.ip_address
}

output "instance_groups" {
  description = "Instance group URLs"
  value = {
    primary   = google_compute_region_instance_group_manager.web_igm_primary.instance_group
    secondary = google_compute_region_instance_group_manager.web_igm_secondary.instance_group
  }
}

```
provider "google" {
  project = "your-gcp-project-id"
  region  = "us-central1"
}

# --- VPC ---
resource "google_compute_network" "main" {
  name                    = "main-vpc"
  auto_create_subnetworks = false
}

# --- Subnet ---
resource "google_compute_subnetwork" "main" {
  name          = "main-subnet"
  ip_cidr_range = "10.0.1.0/24"
  region        = "us-central1"
  network       = google_compute_network.main.id
}

# --- Private DNS Zones ---
resource "google_dns_managed_zone" "example_com" {
  name        = "example-com-zone"
  dns_name    = "example.com."
  description = "Private zone for example.com"
  visibility  = "private"

  private_visibility_config {
    networks {
      network_url = google_compute_network.main.self_link
    }
  }
}

resource "google_dns_managed_zone" "example_org" {
  name        = "example-org-zone"
  dns_name    = "example.org."
  description = "Private zone for example.org"
  visibility  = "private"

  private_visibility_config {
    networks {
      network_url = google_compute_network.main.self_link
    }
  }
}

resource "google_dns_managed_zone" "example_net" {
  name        = "example-net-zone"
  dns_name    = "example.net."
  description = "Private zone for example.net"
  visibility  = "private"

  private_visibility_config {
    networks {
      network_url = google_compute_network.main.self_link
    }
  }
}

# --- Subdomain Records ---
resource "google_dns_record_set" "prod_example_com" {
  name         = "prod.example.com."
  type         = "A"
  ttl          = 300
  managed_zone = google_dns_managed_zone.example_com.name
  rrdatas      = ["10.0.1.10"] # Example internal IP
}

resource "google_dns_record_set" "dev_example_org" {
  name         = "dev.example.org."
  type         = "A"
  ttl          = 300
  managed_zone = google_dns_managed_zone.example_org.name
  rrdatas      = ["10.0.1.11"]
}

resource "google_dns_record_set" "uat_example_net" {
  name         = "uat.example.net."
  type         = "A"
  ttl          = 300
  managed_zone = google_dns_managed_zone.example_net.name
  rrdatas      = ["10.0.1.12"]
}

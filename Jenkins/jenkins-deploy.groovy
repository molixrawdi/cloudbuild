#!/bin/bash

# Jenkins Post-Installation Configuration Deployment Script
# This script deploys the Jenkins configuration after fresh installation

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration variables
JENKINS_HOME="${JENKINS_HOME:-/var/lib/jenkins}"
JENKINS_USER="${JENKINS_USER:-jenkins}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo -e "${GREEN}=== Jenkins Post-Installation Configuration Deployment ===${NC}"

# Function to check if Jenkins is installed
check_jenkins_installed() {
    if ! command -v jenkins &> /dev/null && ! systemctl is-active --quiet jenkins; then
        echo -e "${RED}Error: Jenkins is not installed or not running${NC}"
        echo "Please install Jenkins first and ensure it's running"
        exit 1
    fi
    echo -e "${GREEN}✓ Jenkins installation detected${NC}"
}

# Function to check if running as root or with sudo
check_permissions() {
    if [[ $EUID -ne 0 ]]; then
        echo -e "${RED}Error: This script must be run as root or with sudo${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Running with appropriate permissions${NC}"
}

# Function to backup existing configuration
backup_config() {
    local backup_dir="/tmp/jenkins_backup_$(date +%Y%m%d_%H%M%S)"
    
    if [[ -d "${JENKINS_HOME}/init.groovy.d" ]]; then
        echo -e "${YELLOW}Creating backup of existing configuration...${NC}"
        mkdir -p "$backup_dir"
        cp -r "${JENKINS_HOME}/init.groovy.d" "$backup_dir/"
        echo -e "${GREEN}✓ Backup created at: $backup_dir${NC}"
    fi
}

# Function to deploy configuration script
deploy_config_script() {
    echo -e "${YELLOW}Deploying Jenkins configuration script...${NC}"
    
    # Create init.groovy.d directory if it doesn't exist
    mkdir -p "${JENKINS_HOME}/init.groovy.d"
    
    # Copy the setup script
    if [[ -f "${SCRIPT_DIR}/setup.groovy" ]]; then
        cp "${SCRIPT_DIR}/setup.groovy" "${JENKINS_HOME}/init.groovy.d/"
    else
        echo -e "${RED}Error: setup.groovy not found in script directory${NC}"
        echo "Please ensure setup.groovy is in the same directory as this script"
        exit 1
    fi
    
    # Set proper ownership and permissions
    chown -R ${JENKINS_USER}:${JENKINS_USER} "${JENKINS_HOME}/init.groovy.d"
    chmod 644 "${JENKINS_HOME}/init.groovy.d/setup.groovy"
    
    echo -e "${GREEN}✓ Configuration script deployed${NC}"
}

# Function to set environment variables
setup_environment() {
    echo -e "${YELLOW}Setting up environment variables...${NC}"
    
    # Create environment file if it doesn't exist
    local env_file="/etc/default/jenkins"
    
    if [[ ! -f "$env_file" ]]; then
        touch "$env_file"
    fi
    
    # Add environment variables if not already present
    local vars=(
        "JENKINS_ADMIN_USER=admin"
        "JENKINS_ADMIN_PASSWORD=admin123"
        "JENKINS_URL=http://localhost:8080"
        "SHARED_LIBRARY_REPO=https://github.com/your-org/jenkins-shared-library.git"
        "SHARED_LIBRARY_VERSION=main"
    )
    
    for var in "${vars[@]}"; do
        local key=$(echo "$var" | cut -d'=' -f1)
        if ! grep -q "^export $key=" "$env_file"; then
            echo "export $var" >> "$env_file"
        fi
    done
    
    echo -e "${GREEN}✓ Environment variables configured in $env_file${NC}"
    echo -e "${YELLOW}Note: Please update the values in $env_file according to your setup${NC}"
}

# Function to create sample pipeline scripts directory
create_pipeline_scripts() {
    echo -e "${YELLOW}Creating pipeline scripts directory...${NC}"
    
    local pipeline_dir="${JENKINS_HOME}/pipeline-scripts"
    mkdir -p "$pipeline_dir"
    
    # Create sample pipeline script
    cat > "${pipeline_dir}/sample-cicd.groovy" << 'EOF'
@Library('shared-library@main') _

pipeline {
    agent any
    
    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'staging', 'prod'],
            description: 'Target environment'
        )
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out code...'
            }
        }
        
        stage('Build') {
            steps {
                echo 'Building application...'
            }
        }
        
        stage('Test') {
            steps {
                echo 'Running tests...'
            }
        }
        
        stage('Deploy') {
            steps {
                echo "Deploying to ${params.ENVIRONMENT}..."
            }
        }
    }
}
EOF
    
    chown -R ${JENKINS_USER}:${JENKINS_USER} "$pipeline_dir"
    echo -e "${GREEN}✓ Pipeline scripts directory created${NC}"
}

# Function to create JCasC configuration
create_jcasc_config() {
    echo -e "${YELLOW}Creating Jenkins Configuration as Code (JCasC) template...${NC}"
    
    local jcasc_dir="${JENKINS_HOME}/casc_configs"
    mkdir -p "$jcasc_dir"
    
    cat > "${jcasc_dir}/jenkins.yaml" << 'EOF'
jenkins:
  numExecutors: 2
  mode: NORMAL
  
  securityRealm:
    local:
      allowsSignup: false
      users:
       - id: "admin"
         password: "${JENKINS_ADMIN_PASSWORD:-admin123}"
         
  authorizationStrategy:
    loggedInUsersCanDoAnything:
      allowAnonymousRead: false

unclassified:
  location:
    url: "${JENKINS_URL:-http://localhost:8080}"
    
  globalLibraries:
    libraries:
    - name: "shared-library"
      defaultVersion: "${SHARED_LIBRARY_VERSION:-main}"
      implicit: true
      allowVersionOverride: true
      includeInChangesets: true
      retriever:
        modernSCM:
          scm:
            git:
              remote: "${SHARED_LIBRARY_REPO:-https://github.com/your-org/jenkins-shared-library.git}"

tool:
  git:
    installations:
    - name: "Default"
      home: "git"
EOF
    
    chown -R ${JENKINS_USER}:${JENKINS_USER} "$jcasc_dir"
    echo -e "${GREEN}✓ JCasC configuration template created${NC}"
}

# Function to restart Jenkins
restart_jenkins() {
    echo -e "${YELLOW}Restarting Jenkins to apply configuration...${NC}"
    
    if systemctl is-active --quiet jenkins; then
        systemctl restart jenkins
        
        # Wait for Jenkins to start
        echo -e "${YELLOW}Waiting for Jenkins to start...${NC}"
        local count=0
        while ! curl -s http://localhost:8080/login > /dev/null; do
            sleep 5
            count=$((count + 1))
            if [[ $count -gt 24 ]]; then  # 2 minutes timeout
                echo -e "${RED}Error: Jenkins failed to start within 2 minutes${NC}"
                exit 1
            fi
            echo -n "."
        done
        echo ""
        echo -e "${GREEN}✓ Jenkins restarted successfully${NC}"
    else
        echo -e "${YELLOW}Starting Jenkins...${NC}"
        systemctl start jenkins
        echo -e "${GREEN}✓ Jenkins started${NC}"
    fi
}

# Function to verify deployment
verify_deployment() {
    echo -e "${YELLOW}Verifying deployment...${NC}"
    
    local jenkins_url="http://localhost:8080"
    
    # Check if Jenkins is responding
    if curl -s "$jenkins_url/login" > /dev/null; then
        echo -e "${GREEN}✓ Jenkins is responding at $jenkins_url${NC}"
    else
        echo -e "${RED}✗ Jenkins is not responding${NC}"
        return 1
    fi
    
    # Check if configuration files exist
    local files=(
        "${JENKINS_HOME}/init.groovy.d/setup.groovy"
        "${JENKINS_HOME}/pipeline-scripts/sample-cicd.groovy"
        "${JENKINS_HOME}/casc_configs/jenkins.yaml"
    )
    
    for file in "${files[@]}"; do
        if [[ -f "$file" ]]; then
            echo -e "${GREEN}✓ $file exists${NC}"
        else
            echo -e "${RED}✗ $file missing${NC}"
        fi
    done
}

# Function to display next steps
show_next_steps() {
    echo -e "\n${GREEN}=== Deployment Complete ===${NC}"
    echo -e "${YELLOW}Next Steps:${NC}"
    echo "1. Access Jenkins at: http://localhost:8080"
    echo "2. Login with username: admin, password: admin123 (or your configured password)"
    echo "3. Update environment variables in: /etc/default/jenkins"
    echo "4. Configure your shared library repository URL"
    echo "5. Set up proper credentials for GitHub/Git access"
    echo "6. Review and customize the sample pipelines"
    echo ""
    echo -e "${YELLOW}Configuration Files:${NC}"
    echo "- Main config script: ${JENKINS_HOME}/init.groovy.d/setup.groovy"
    echo "- Pipeline scripts: ${JENKINS_HOME}/pipeline-scripts/"
    echo "- JCasC config: ${JENKINS_HOME}/casc_configs/jenkins.yaml"
    echo "- Environment vars: /etc/default/jenkins"
    echo ""
    echo -e "${YELLOW}Important:${NC}"
    echo "- Change the default admin password"
    echo "- Update shared library repository URLs"
    echo "- Configure proper authentication"
    echo "- Set up firewall rules for port 8080"
}

# Main execution
main() {
    check_jenkins_installed
    check_permissions
    backup_config
    deploy_config_script
    setup_environment
    create_pipeline_scripts
    create_jcasc_config
    restart_jenkins
    
    if verify_deployment; then
        show_next_steps
    else
        echo -e "${RED}Deployment verification failed. Please check the logs for errors.${NC}"
        exit 1
    fi
}
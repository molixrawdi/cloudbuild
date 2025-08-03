#!/usr/bin/env groovy

/*
 * Jenkins Post-Installation Configuration Script
 * Place this in /var/lib/jenkins/init.groovy.d/setup.groovy
 * This script runs automatically after Jenkins starts
 */

import jenkins.model.*
import hudson.security.*
import hudson.security.csrf.DefaultCrumbIssuer
import jenkins.security.s2m.AdminWhitelistRule
import org.jenkinsci.plugins.workflow.libs.*
import jenkins.plugins.git.GitSCMSource
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.impl.*
import hudson.plugins.git.*
import hudson.model.*
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import jenkins.branch.BranchSource
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource

def instance = Jenkins.getInstance()

println "=== Starting Jenkins Post-Install Configuration ==="

// 1. BASIC SECURITY SETUP
println "Setting up basic security..."

// Enable CSRF protection
instance.setCrumbIssuer(new DefaultCrumbIssuer(true))

// Set security realm (using Jenkins database)
def hudsonRealm = new HudsonPrivateSecurityRealm(false)
instance.setSecurityRealm(hudsonRealm)

// Create admin user if not exists
def adminUsername = System.getenv('JENKINS_ADMIN_USER') ?: 'admin'
def adminPassword = System.getenv('JENKINS_ADMIN_PASSWORD') ?: 'admin123'

if (!hudsonRealm.getAllUsers().contains(hudsonRealm.loadUserByUsername(adminUsername))) {
    hudsonRealm.createAccount(adminUsername, adminPassword)
    println "Created admin user: ${adminUsername}"
}

// Set authorization strategy
def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
strategy.setAllowAnonymousRead(false)
instance.setAuthorizationStrategy(strategy)

// 2. CONFIGURE GLOBAL SETTINGS
println "Configuring global settings..."

// Set number of executors
instance.setNumExecutors(2)

// Set Jenkins URL (adjust as needed)
def jenkinsUrl = System.getenv('JENKINS_URL') ?: 'http://localhost:8080'
def locationConfig = JenkinsLocationConfiguration.get()
locationConfig.setUrl(jenkinsUrl)
locationConfig.save()

// 3. ADD CREDENTIALS (if environment variables are provided)
println "Setting up credentials..."

def domain = Domain.global()
def store = instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()

// GitHub credentials
def githubToken = System.getenv('GITHUB_TOKEN')
if (githubToken) {
    def githubCredentials = new StringCredentialsImpl(
        CredentialsScope.GLOBAL,
        "github-token",
        "GitHub Personal Access Token",
        Secret.fromString(githubToken)
    )
    store.addCredentials(domain, githubCredentials)
    println "Added GitHub token credentials"
}

// Git SSH credentials
def gitSshKey = System.getenv('GIT_SSH_PRIVATE_KEY')
if (gitSshKey) {
    def sshCredentials = new BasicSSHUserPrivateKey(
        CredentialsScope.GLOBAL,
        "git-ssh-key",
        "git",
        new BasicSSHUserPrivateKey.DirectEntryPrivateKeySource(gitSshKey),
        "",
        "Git SSH Private Key"
    )
    store.addCredentials(domain, sshCredentials)
    println "Added Git SSH credentials"
}

// 4. CONFIGURE SHARED LIBRARIES
println "Setting up shared libraries..."

def globalLibraries = GlobalLibraries.get()
def libraries = []

// Main shared library
def sharedLibConfig = new LibraryConfiguration("shared-library", 
    new SCMSourceRetriever(new GitSCMSource(
        System.getenv('SHARED_LIBRARY_REPO') ?: 'https://github.com/your-org/jenkins-shared-library.git'
    )))
sharedLibConfig.setDefaultVersion(System.getenv('SHARED_LIBRARY_VERSION') ?: 'main')
sharedLibConfig.setImplicit(true)
sharedLibConfig.setAllowVersionOverride(true)
sharedLibConfig.setIncludeInChangesets(true)

// Set credentials if available
if (githubToken) {
    sharedLibConfig.getRetriever().getScm().setCredentialsId("github-token")
}

libraries.add(sharedLibConfig)

// Additional shared library (optional)
def utilsLibRepo = System.getenv('UTILS_LIBRARY_REPO')
if (utilsLibRepo) {
    def utilsLibConfig = new LibraryConfiguration("utils-library", 
        new SCMSourceRetriever(new GitSCMSource(utilsLibRepo)))
    utilsLibConfig.setDefaultVersion("main")
    utilsLibConfig.setImplicit(false)
    utilsLibConfig.setAllowVersionOverride(true)
    libraries.add(utilsLibConfig)
    println "Added utils library: ${utilsLibRepo}"
}

globalLibraries.setLibraries(libraries)
println "Configured ${libraries.size()} shared libraries"

// 5. CREATE SAMPLE PIPELINES
println "Creating sample pipelines..."

// Create a folder for pipelines
def pipelinesFolder = instance.createProject(Folder.class, "pipelines")
pipelinesFolder.setDescription("Container for all pipeline jobs")

// Sample Pipeline Job 1: Simple CI/CD
def job1 = pipelinesFolder.createProject(WorkflowJob.class, "sample-cicd-pipeline")
job1.setDescription("Sample CI/CD Pipeline using shared library")

def pipelineScript1 = '''
@Library('shared-library@main') _

pipeline {
    agent any
    
    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'staging', 'prod'],
            description: 'Target environment'
        )
        booleanParam(
            name: 'DEPLOY',
            defaultValue: false,
            description: 'Deploy after build?'
        )
    }
    
    environment {
        APP_NAME = 'my-application'
        BUILD_NUMBER = "${BUILD_NUMBER}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                script {
                    // Using shared library function
                    buildApplication(env.APP_NAME)
                }
            }
        }
        
        stage('Test') {
            parallel {
                stage('Unit Tests') {
                    steps {
                        sh 'echo "Running unit tests..."'
                        // runUnitTests() // shared library function
                    }
                }
                stage('Integration Tests') {
                    steps {
                        sh 'echo "Running integration tests..."'
                        // runIntegrationTests() // shared library function
                    }
                }
            }
        }
        
        stage('Security Scan') {
            steps {
                script {
                    // securityScan(env.APP_NAME) // shared library function
                    sh 'echo "Running security scan..."'
                }
            }
        }
        
        stage('Deploy') {
            when {
                expression { params.DEPLOY == true }
            }
            steps {
                script {
                    // deployApplication(env.APP_NAME, params.ENVIRONMENT) // shared library
                    echo "Deploying ${env.APP_NAME} to ${params.ENVIRONMENT}"
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            echo "Pipeline completed successfully!"
        }
        failure {
            echo "Pipeline failed!"
        }
    }
}
'''

job1.setDefinition(new CpsFlowDefinition(pipelineScript1, true))

// Sample Pipeline Job 2: Multi-branch pipeline
def multiBranchProject = pipelinesFolder.createProject(WorkflowMultiBranchProject.class, "multi-branch-pipeline")
multiBranchProject.setDescription("Multi-branch pipeline for feature development")

// Configure Git source for multi-branch
def gitSCMSource = new GitSCMSource(System.getenv('APP_REPO') ?: 'https://github.com/your-org/your-app.git')
if (githubToken) {
    gitSCMSource.setCredentialsId("github-token")
}

def branchSource = new BranchSource(gitSCMSource)
multiBranchProject.getSourcesList().add(branchSource)

// 6. CREATE VIEWS
println "Creating views..."

// Create a view for pipelines
def pipelineView = new hudson.model.ListView("Pipeline Jobs")
pipelineView.setIncludeRegex(".*pipeline.*")
instance.addView(pipelineView)

// 7. INSTALL ESSENTIAL PLUGINS (if not already installed)
println "Checking essential plugins..."

def pluginManager = instance.getPluginManager()
def updateCenter = instance.getUpdateCenter()

def essentialPlugins = [
    'workflow-aggregator',
    'git',
    'github',
    'pipeline-stage-view',
    'blueocean',
    'credentials-binding',
    'timestamper',
    'ws-cleanup',
    'build-timeout'
]

essentialPlugins.each { pluginName ->
    if (!pluginManager.getPlugin(pluginName)) {
        println "Plugin ${pluginName} not found - would need manual installation"
    }
}

// Save all configurations
instance.save()

println "=== Jenkins Post-Install Configuration Complete ==="
println "Admin user: ${adminUsername}"
println "Jenkins URL: ${jenkinsUrl}"
println "Shared libraries configured: ${libraries.size()}"
println "Sample pipelines created in 'pipelines' folder"
println ""
println "Next steps:"
println "1. Update shared library repository URL in the configuration"
println "2. Add your application repositories"
println "3. Configure webhooks for automatic builds"
println "4. Review and customize the sample pipelines"
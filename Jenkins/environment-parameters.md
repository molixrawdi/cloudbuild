# Jenkins Environment vs Parameters - Complete Comparison

## Key Differences Overview

| Aspect | Parameters | Environment |
|--------|------------|-------------|
| **Purpose** | User input for build customization | Variables available during build execution |
| **When Set** | Before build starts (user input) | During pipeline definition/execution |
| **User Interaction** | Interactive UI prompts | No user interaction |
| **Scope** | Pipeline-wide, accessible as `params.NAME` | Pipeline/stage/step scoped as `env.NAME` |
| **Mutability** | Read-only during execution | Can be modified during execution |
| **Persistence** | Only for current build | Only for current build |

## Parameters - User Input

Parameters are **input fields** that users fill out when triggering a build.

```groovy
pipeline {
    agent any
    
    parameters {
        string(
            name: 'APP_VERSION',
            defaultValue: '1.0.0',
            description: 'Version to deploy'
        )
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'staging', 'prod'],
            description: 'Target environment'
        )
        booleanParam(
            name: 'SKIP_TESTS',
            defaultValue: false,
            description: 'Skip running tests'
        )
    }
    
    stages {
        stage('Show Parameters') {
            steps {
                script {
                    // Parameters are READ-ONLY
                    echo "User selected version: ${params.APP_VERSION}"
                    echo "Target environment: ${params.ENVIRONMENT}"
                    echo "Skip tests: ${params.SKIP_TESTS}"
                    
                    // This would FAIL - parameters are immutable
                    // params.APP_VERSION = "2.0.0"  // ERROR!
                }
            }
        }
    }
}
```

## Environment - Build Variables

Environment variables are **key-value pairs** available during build execution.

```groovy
pipeline {
    agent any
    
    // Global environment variables
    environment {
        DOCKER_REGISTRY = 'registry.example.com'
        BUILD_TIMESTAMP = sh(script: 'date +%Y%m%d-%H%M%S', returnStdout: true).trim()
        APP_NAME = 'my-flask-app'
    }
    
    stages {
        stage('Global Environment') {
            steps {
                script {
                    // Environment variables are accessible
                    echo "Registry: ${env.DOCKER_REGISTRY}"
                    echo "Build time: ${env.BUILD_TIMESTAMP}"
                    
                    // Environment variables can be MODIFIED
                    env.DOCKER_TAG = "${env.APP_NAME}:${env.BUILD_TIMESTAMP}"
                    echo "New tag: ${env.DOCKER_TAG}"
                }
            }
        }
        
        stage('Stage-Specific Environment') {
            environment {
                // Stage-specific environment variables
                STAGE_NAME = 'build'
                DEBUG_LEVEL = 'verbose'
            }
            steps {
                echo "Stage: ${env.STAGE_NAME}"
                echo "Debug: ${env.DEBUG_LEVEL}"
            }
        }
    }
}
```

## Combining Parameters and Environment

The most powerful pattern is using parameters to set environment variables:

```groovy
pipeline {
    agent any
    
    parameters {
        choice(
            name: 'DEPLOY_ENV',
            choices: ['dev', 'staging', 'prod'],
            description: 'Deployment environment'
        )
        string(
            name: 'APP_VERSION',
            defaultValue: '1.0.0',
            description: 'Application version'
        )
        booleanParam(
            name: 'ENABLE_MONITORING',
            defaultValue: true,
            description: 'Enable monitoring'
        )
    }
    
    environment {
        // Use parameters to set environment variables
        TARGET_ENVIRONMENT = "${params.DEPLOY_ENV}"
        VERSION_TAG = "${params.APP_VERSION}"
        
        // Conditional environment based on parameters
        REGISTRY_URL = "${params.DEPLOY_ENV == 'prod' ? 'prod-registry.com' : 'dev-registry.com'}"
        
        // Static environment variables
        BUILD_ID = "${BUILD_NUMBER}"
        WORKSPACE_PATH = "${WORKSPACE}"
        
        // Dynamic environment variables
        DOCKER_IMAGE = "${params.DEPLOY_ENV}-app:${params.APP_VERSION}-${BUILD_NUMBER}"
    }
    
    stages {
        stage('Environment Setup') {
            steps {
                script {
                    echo "=== Parameters (User Input) ==="
                    echo "Deploy Environment: ${params.DEPLOY_ENV}"
                    echo "App Version: ${params.APP_VERSION}"
                    echo "Enable Monitoring: ${params.ENABLE_MONITORING}"
                    
                    echo "=== Environment Variables ==="
                    echo "Target Environment: ${env.TARGET_ENVIRONMENT}"
                    echo "Registry URL: ${env.REGISTRY_URL}"
                    echo "Docker Image: ${env.DOCKER_IMAGE}"
                    echo "Build ID: ${env.BUILD_ID}"
                    
                    // Modify environment based on parameters
                    if (params.ENABLE_MONITORING) {
                        env.MONITORING_CONFIG = 'enabled'
                        env.METRICS_ENDPOINT = "https://metrics-${params.DEPLOY_ENV}.example.com"
                    } else {
                        env.MONITORING_CONFIG = 'disabled'
                    }
                    
                    echo "Monitoring: ${env.MONITORING_CONFIG}"
                }
            }
        }
        
        stage('Environment-Specific Logic') {
            steps {
                script {
                    // Use both parameters and environment
                    switch(params.DEPLOY_ENV) {
                        case 'prod':
                            env.RESOURCE_LIMITS = 'high'
                            env.REPLICA_COUNT = '3'
                            env.LOG_LEVEL = 'warn'
                            break
                        case 'staging':
                            env.RESOURCE_LIMITS = 'medium'
                            env.REPLICA_COUNT = '2'
                            env.LOG_LEVEL = 'info'
                            break
                        case 'dev':
                            env.RESOURCE_LIMITS = 'low'
                            env.REPLICA_COUNT = '1'
                            env.LOG_LEVEL = 'debug'
                            break
                    }
                    
                    echo "Resource limits: ${env.RESOURCE_LIMITS}"
                    echo "Replicas: ${env.REPLICA_COUNT}"
                    echo "Log level: ${env.LOG_LEVEL}"
                }
            }
        }
    }
}
```

## Built-in Environment Variables

Jenkins provides many built-in environment variables:

```groovy
pipeline {
    agent any
    
    stages {
        stage('Built-in Environment') {
            steps {
                script {
                    echo "=== Jenkins Built-in Environment Variables ==="
                    echo "Job Name: ${env.JOB_NAME}"
                    echo "Build Number: ${env.BUILD_NUMBER}"
                    echo "Build ID: ${env.BUILD_ID}"
                    echo "Build URL: ${env.BUILD_URL}"
                    echo "Workspace: ${env.WORKSPACE}"
                    echo "Jenkins URL: ${env.JENKINS_URL}"
                    echo "Node Name: ${env.NODE_NAME}"
                    echo "Git Branch: ${env.GIT_BRANCH}"
                    echo "Git Commit: ${env.GIT_COMMIT}"
                    
                    // Print all environment variables
                    echo "=== All Environment Variables ==="
                    sh 'printenv | sort'
                }
            }
        }
    }
}
```

## Scope Differences

### Parameters Scope
- **Global**: Available throughout entire pipeline
- **Immutable**: Cannot be changed during execution
- **Accessed via**: `params.PARAMETER_NAME`

### Environment Scope
- **Pipeline-level**: Available in all stages
- **Stage-level**: Available only in specific stage
- **Step-level**: Can be set in individual steps
- **Mutable**: Can be modified during execution
- **Accessed via**: `env.VARIABLE_NAME` or `${VARIABLE_NAME}`

```groovy
pipeline {
    agent any
    
    parameters {
        string(name: 'GLOBAL_PARAM', defaultValue: 'param-value')
    }
    
    environment {
        GLOBAL_ENV = 'global-env-value'
    }
    
    stages {
        stage('Stage 1') {
            environment {
                STAGE_ENV = 'stage1-env-value'
            }
            steps {
                script {
                    // Parameter: Available everywhere, immutable
                    echo "Global param: ${params.GLOBAL_PARAM}"
                    
                    // Global environment: Available everywhere, mutable
                    echo "Global env: ${env.GLOBAL_ENV}"
                    env.GLOBAL_ENV = 'modified-global-env'
                    
                    // Stage environment: Only in this stage
                    echo "Stage env: ${env.STAGE_ENV}"
                    
                    // Step-level environment
                    env.STEP_ENV = 'step-env-value'
                    echo "Step env: ${env.STEP_ENV}"
                }
            }
        }
        
        stage('Stage 2') {
            steps {
                script {
                    // Parameter: Still available
                    echo "Global param: ${params.GLOBAL_PARAM}"
                    
                    // Modified global environment: Available
                    echo "Modified global env: ${env.GLOBAL_ENV}"
                    
                    // Stage environment from Stage 1: NOT available
                    echo "Stage env from Stage 1: ${env.STAGE_ENV}" // Will be null
                    
                    // Step environment from Stage 1: Available (if set globally)
                    echo "Step env from Stage 1: ${env.STEP_ENV}"
                }
            }
        }
    }
}
```

## Best Practices

### Use Parameters For:
- ✅ User input and build customization
- ✅ Configuration that changes between builds
- ✅ Deployment targets and versions
- ✅ Feature flags and toggles

### Use Environment For:
- ✅ Computed values during build
- ✅ Build metadata and timestamps
- ✅ Tool paths and configurations
- ✅ Temporary variables during execution

### Combined Pattern:
```groovy
pipeline {
    agent any
    
    parameters {
        // User inputs
        choice(name: 'ENVIRONMENT', choices: ['dev', 'prod'])
        string(name: 'VERSION', defaultValue: '1.0.0')
    }
    
    environment {
        // Computed from parameters
        DEPLOY_TARGET = "${params.ENVIRONMENT}"
        IMAGE_TAG = "${params.VERSION}-${BUILD_NUMBER}"
        
        // Build-specific
        BUILD_TIMESTAMP = sh(script: 'date -u +%Y%m%d%H%M%S', returnStdout: true).trim()
        
        // Tool configurations
        DOCKER_BUILDKIT = '1'
        COMPOSE_DOCKER_CLI_BUILD = '1'
    }
    
    stages {
        stage('Build') {
            steps {
                script {
                    // Use both parameters and environment
                    def fullImageName = "${env.DEPLOY_TARGET}-app:${env.IMAGE_TAG}"
                    sh "docker build -t ${fullImageName} ."
                    
                    // Set new environment variable for next stages
                    env.BUILT_IMAGE = fullImageName
                }
            }
        }
    }
}
```

## Summary

- **Parameters** = User input before build starts (immutable)
- **Environment** = Variables during build execution (mutable)
- **Best Practice** = Use parameters for user choices, environment for computed values
- **Access** = `params.NAME` for parameters, `env.NAME` for environment
- **Scope** = Parameters are global, environment can be pipeline/stage/step scoped
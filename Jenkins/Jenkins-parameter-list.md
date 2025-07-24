
# Complete Jenkins Pipeline Parameters Reference

## Parameter Types

### 1. `string` Parameter
Basic text input parameter.

```groovy
parameters {
    string(
        name: 'PARAMETER_NAME',
        defaultValue: 'default_value',
        description: 'Description of the parameter',
        trim: true  // Optional: removes leading/trailing whitespace
    )
}
```

**Properties:**
- `name` (required): Parameter name
- `defaultValue` (optional): Default value
- `description` (optional): Help text
- `trim` (optional): Boolean, removes whitespace

### 2. `text` Parameter
Multi-line text input parameter.

```groovy
parameters {
    text(
        name: 'MULTI_LINE_TEXT',
        defaultValue: 'Line 1\nLine 2\nLine 3',
        description: 'Multi-line text parameter'
    )
}
```

**Properties:**
- `name` (required): Parameter name
- `defaultValue` (optional): Default multi-line value
- `description` (optional): Help text

### 3. `booleanParam` Parameter
Checkbox parameter (true/false).

```groovy
parameters {
    booleanParam(
        name: 'ENABLE_FEATURE',
        defaultValue: true,
        description: 'Enable this feature'
    )
}
```

**Properties:**
- `name` (required): Parameter name
- `defaultValue` (optional): Boolean default value
- `description` (optional): Help text

### 4. `choice` Parameter
Dropdown selection parameter.

```groovy
parameters {
    choice(
        name: 'ENVIRONMENT',
        choices: ['dev', 'staging', 'production'],
        description: 'Select deployment environment'
    )
}
```

**Properties:**
- `name` (required): Parameter name
- `choices` (required): List of options
- `description` (optional): Help text

### 5. `password` Parameter
Password input (masked in UI).

```groovy
parameters {
    password(
        name: 'SECRET_KEY',
        defaultValue: '',
        description: 'Secret key for deployment'
    )
}
```

**Properties:**
- `name` (required): Parameter name
- `defaultValue` (optional): Default value (usually empty)
- `description` (optional): Help text

### 6. `file` Parameter
File upload parameter.

```groovy
parameters {
    file(
        name: 'CONFIG_FILE',
        description: 'Upload configuration file'
    )
}
```

**Properties:**
- `name` (required): Parameter name
- `description` (optional): Help text

**Usage in pipeline:**
```groovy
steps {
    script {
        def configFile = params.CONFIG_FILE
        writeFile file: 'uploaded-config.json', text: configFile
    }
}
```

### 7. `run` Parameter
Select a build from another job.

```groovy
parameters {
    run(
        name: 'UPSTREAM_BUILD',
        project: 'upstream-job-name',
        description: 'Select upstream build',
        filter: 'SUCCESSFUL'  // Optional: SUCCESSFUL, STABLE, UNSTABLE, etc.
    )
}
```

**Properties:**
- `name` (required): Parameter name
- `project` (required): Name of the upstream project
- `description` (optional): Help text
- `filter` (optional): Build status filter

### 8. Custom Parameter Types (via plugins)

#### `gitParameter` (Git Parameter Plugin)
Select Git branches, tags, or commits.

```groovy
parameters {
    gitParameter(
        name: 'BRANCH',
        type: 'PT_BRANCH',
        defaultValue: 'main',
        description: 'Select branch to build',
        branch: '',
        branchFilter: 'origin/(.*)',
        tagFilter: '*',
        sortMode: 'NONE',
        selectedValue: 'DEFAULT',
        useRepository: '.',
        quickFilterEnabled: true
    )
}
```

**Types:**
- `PT_BRANCH`: Branches
- `PT_TAG`: Tags
- `PT_REVISION`: Commits
- `PT_PULL_REQUEST`: Pull requests

#### `activeChoice` (Active Choices Plugin)
Dynamic choice parameter.

```groovy
parameters {
    activeChoice(
        name: 'DYNAMIC_CHOICE',
        description: 'Dynamic choice parameter',
        filterable: true,
        choiceType: 'SINGLE_SELECT',
        script: [
            $class: 'GroovyScript',
            script: [
                sandbox: false,
                script: '''
                    return ['option1', 'option2', 'option3']
                '''
            ]
        ]
    )
}
```

#### `cascadeChoiceParameter` (Active Choices Plugin)
Cascading choice parameter.

```groovy
parameters {
    choice(
        name: 'REGION',
        choices: ['us-east-1', 'us-west-2', 'eu-west-1'],
        description: 'Select AWS region'
    )
    cascadeChoiceParameter(
        name: 'AVAILABILITY_ZONE',
        description: 'Select availability zone',
        referencedParameters: 'REGION',
        choiceType: 'SINGLE_SELECT',
        script: [
            $class: 'GroovyScript',
            script: [
                sandbox: false,
                script: '''
                    if (REGION.equals('us-east-1')) {
                        return ['us-east-1a', 'us-east-1b', 'us-east-1c']
                    } else if (REGION.equals('us-west-2')) {
                        return ['us-west-2a', 'us-west-2b', 'us-west-2c']
                    } else if (REGION.equals('eu-west-1')) {
                        return ['eu-west-1a', 'eu-west-1b', 'eu-west-1c']
                    }
                    return ['No zones available']
                '''
            ]
        ]
    )
}
```

## Complete Example Pipeline

```groovy
pipeline {
    agent any
    
    parameters {
        // Basic parameters
        string(
            name: 'APPLICATION_NAME',
            defaultValue: 'my-app',
            description: 'Name of the application',
            trim: true
        )
        
        text(
            name: 'RELEASE_NOTES',
            defaultValue: 'Bug fixes and improvements',
            description: 'Release notes for this deployment'
        )
        
        booleanParam(
            name: 'SKIP_TESTS',
            defaultValue: false,
            description: 'Skip running tests'
        )
        
        choice(
            name: 'ENVIRONMENT',
            choices: ['development', 'staging', 'production'],
            description: 'Target environment'
        )
        
        choice(
            name: 'PYTHON_VERSION',
            choices: ['3.9', '3.10', '3.11', '3.12'],
            description: 'Python version for Docker image'
        )
        
        choice(
            name: 'BASE_IMAGE',
            choices: ['python', 'python-slim', 'python-alpine'],
            description: 'Base Docker image type'
        )
        
        password(
            name: 'DEPLOYMENT_KEY',
            defaultValue: '',
            description: 'Deployment authentication key'
        )
        
        file(
            name: 'CONFIG_FILE',
            description: 'Upload application configuration file'
        )
        
        // Git parameter (requires plugin)
        gitParameter(
            name: 'GIT_BRANCH',
            type: 'PT_BRANCH',
            defaultValue: 'main',
            description: 'Git branch to build',
            branchFilter: 'origin/(.*)',
            quickFilterEnabled: true
        )
        
        // Active choice parameter (requires plugin)
        activeChoice(
            name: 'BUILD_TOOL',
            description: 'Select build tool',
            choiceType: 'SINGLE_SELECT',
            script: [
                $class: 'GroovyScript',
                script: [
                    sandbox: false,
                    script: '''
                        def tools = []
                        if (new File('/usr/bin/docker').exists()) {
                            tools.add('docker')
                        }
                        if (new File('/usr/bin/podman').exists()) {
                            tools.add('podman')
                        }
                        if (new File('/usr/bin/buildah').exists()) {
                            tools.add('buildah')
                        }
                        return tools.isEmpty() ? ['none-available'] : tools
                    '''
                ]
            ]
        )
    }
    
    environment {
        // Use parameters in environment variables
        APP_NAME = "${params.APPLICATION_NAME}"
        TARGET_ENV = "${params.ENVIRONMENT}"
        DOCKER_IMAGE = "${params.APPLICATION_NAME}:${params.PYTHON_VERSION}-${BUILD_NUMBER}"
        PYTHON_BASE_IMAGE = "${params.BASE_IMAGE}:${params.PYTHON_VERSION}"
    }
    
    stages {
        stage('Parameter Summary') {
            steps {
                script {
                    echo "=== Build Parameters ==="
                    echo "Application: ${params.APPLICATION_NAME}"
                    echo "Environment: ${params.ENVIRONMENT}"
                    echo "Python Version: ${params.PYTHON_VERSION}"
                    echo "Base Image: ${params.BASE_IMAGE}"
                    echo "Skip Tests: ${params.SKIP_TESTS}"
                    echo "Git Branch: ${params.GIT_BRANCH}"
                    echo "Build Tool: ${params.BUILD_TOOL}"
                    echo "Release Notes: ${params.RELEASE_NOTES}"
                    
                    // Handle file parameter
                    if (params.CONFIG_FILE) {
                        writeFile file: 'app-config.json', text: params.CONFIG_FILE
                        echo "Configuration file uploaded and saved as app-config.json"
                    }
                }
            }
        }
        
        stage('Checkout') {
            steps {
                script {
                    if (params.GIT_BRANCH) {
                        checkout([
                            $class: 'GitSCM',
                            branches: [[name: "${params.GIT_BRANCH}"]],
                            userRemoteConfigs: scm.userRemoteConfigs
                        ])
                    } else {
                        checkout scm
                    }
                }
            }
        }
        
        stage('Build') {
            steps {
                script {
                    def dockerfile = """
FROM ${env.PYTHON_BASE_IMAGE}

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 5000

CMD ["python", "app.py"]
"""
                    writeFile file: 'Dockerfile.generated', text: dockerfile
                    
                    if (params.BUILD_TOOL == 'docker') {
                        sh "docker build -f Dockerfile.generated -t ${env.DOCKER_IMAGE} ."
                    } else if (params.BUILD_TOOL == 'podman') {
                        sh "podman build -f Dockerfile.generated -t ${env.DOCKER_IMAGE} ."
                    } else {
                        error "Build tool ${params.BUILD_TOOL} not supported"
                    }
                }
            }
        }
        
        stage('Test') {
            when {
                not { params.SKIP_TESTS }
            }
            steps {
                sh "docker run --rm ${env.DOCKER_IMAGE} python -m pytest tests/ -v"
            }
        }
        
        stage('Deploy') {
            when {
                expression { 
                    params.ENVIRONMENT in ['staging', 'production'] 
                }
            }
            steps {
                script {
                    echo "Deploying ${env.DOCKER_IMAGE} to ${params.ENVIRONMENT}"
                    
                    // Use deployment key if provided
                    if (params.DEPLOYMENT_KEY) {
                        echo "Using provided deployment key for authentication"
                        // Use the key for deployment authentication
                    }
                    
                    // Environment-specific deployment logic
                    if (params.ENVIRONMENT == 'production') {
                        echo "Production deployment with extra validations"
                        // Add production-specific steps
                    } else {
                        echo "Staging deployment"
                        // Add staging-specific steps
                    }
                }
            }
        }
    }
    
    post {
        always {
            script {
                // Create deployment report
                def report = """
Deployment Report
================
Application: ${params.APPLICATION_NAME}
Environment: ${params.ENVIRONMENT}
Python Version: ${params.PYTHON_VERSION}
Docker Image: ${env.DOCKER_IMAGE}
Build Status: ${currentBuild.currentResult}
Release Notes: ${params.RELEASE_NOTES}
"""
                writeFile file: 'deployment-report.txt', text: report
                archiveArtifacts artifacts: 'deployment-report.txt', fingerprint: true
            }
        }
        success {
            echo "Deployment successful! 🎉"
        }
        failure {
            echo "Deployment failed! ❌"
        }
    }
}
```

## Parameter Access Patterns

### Accessing Parameters
```groovy
// Direct access
echo "Environment: ${params.ENVIRONMENT}"

// In conditionals
when {
    expression { params.SKIP_TESTS == false }
}

// In environment variables
environment {
    TARGET_ENV = "${params.ENVIRONMENT}"
}

// In script blocks
script {
    if (params.ENVIRONMENT == 'production') {
        // production logic
    }
}
```

### Parameter Validation
```groovy
stage('Validate Parameters') {
    steps {
        script {
            // Validate string parameter
            if (!params.APPLICATION_NAME?.trim()) {
                error "Application name cannot be empty"
            }
            
            // Validate choice parameter
            def validEnvironments = ['dev', 'staging', 'prod']
            if (!(params.ENVIRONMENT in validEnvironments)) {
                error "Invalid environment: ${params.ENVIRONMENT}"
            }
            
            // Validate file parameter
            if (params.CONFIG_FILE && !params.CONFIG_FILE.contains('{')) {
                error "Config file must be valid JSON"
            }
        }
    }
}
```

## Plugin-Specific Parameters

### Extended Choice Parameter Plugin
```groovy
parameters {
    extendedChoice(
        name: 'MULTI_SELECT',
        description: 'Multiple selection parameter',
        type: 'PT_CHECKBOX',
        value: 'option1,option2,option3',
        defaultValue: 'option1,option2',
        multiSelectDelimiter: ','
    )
}
```

### Node and Label Parameter Plugin
```groovy
parameters {
    nodeParam(
        name: 'BUILD_NODE',
        description: 'Select node to run build',
        allowedSlaves: ['node1', 'node2', 'node3'],
        defaultSlaves: ['node1'],
        triggerIfResult: 'multiSelectionDisallowed'
    )
}
```

This comprehensive reference covers all standard Jenkins pipeline parameters plus common plugin-based parameters. Each parameter type has specific use cases and properties that make them suitable for different scenarios in your CI/CD pipelines.
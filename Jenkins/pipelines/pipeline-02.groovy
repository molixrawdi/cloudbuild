pipeline {
    agent any
    
    parameters {
        choice(
            name: 'PYTHON_VERSION',
            choices: ['3.9', '3.10', '3.11', '3.12'],
            description: 'Select Python version for the Docker image'
        )
        choice(
            name: 'BASE_IMAGE',
            choices: ['python', 'python-slim', 'python-alpine'],
            description: 'Select base Python image type'
        )
        string(
            name: 'CUSTOM_TAG',
            defaultValue: 'latest',
            description: 'Custom tag for the built image'
        )
    }
    
    environment {
        DOCKER_IMAGE = "flask-app"
        DOCKER_REGISTRY = "your-registry.com" // Change to your registry
        PYTHON_BASE_IMAGE = "${params.BASE_IMAGE}:${params.PYTHON_VERSION}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Setup Environment') {
            steps {
                script {
                    echo "Building with Python ${params.PYTHON_VERSION} using ${params.BASE_IMAGE} base image"
                    env.FULL_IMAGE_TAG = "${DOCKER_REGISTRY}/${DOCKER_IMAGE}:${params.PYTHON_VERSION}-${params.CUSTOM_TAG}"
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    // Create Dockerfile dynamically or use build args
                    def dockerfile = """
FROM ${PYTHON_BASE_IMAGE}

WORKDIR /app

# Copy requirements first for better caching
COPY requirements.txt .

# Install dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Expose Flask port
EXPOSE 5000

# Set environment variables
ENV FLASK_APP=app.py
ENV FLASK_RUN_HOST=0.0.0.0

# Run the application
CMD ["flask", "run"]
"""
                    writeFile file: 'Dockerfile.generated', text: dockerfile
                    
                    // Build the Docker image
                    docker.build(
                        "${env.FULL_IMAGE_TAG}",
                        "-f Dockerfile.generated ."
                    )
                }
            }
        }
        
        stage('Test Application') {
            parallel {
                stage('Unit Tests') {
                    steps {
                        script {
                            docker.image("${env.FULL_IMAGE_TAG}").inside {
                                sh '''
                                    python -m pytest tests/ -v --junitxml=test-results.xml || true
                                '''
                            }
                        }
                    }
                    post {
                        always {
                            publishTestResults testResultsPattern: 'test-results.xml'
                        }
                    }
                }
                
                stage('Security Scan') {
                    steps {
                        script {
                            // Example using Trivy for container scanning
                            sh """
                                docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \\
                                    -v \$PWD:/tmp/.cache/ aquasec/trivy:latest image \\
                                    --format json --output trivy-report.json \\
                                    ${env.FULL_IMAGE_TAG} || true
                            """
                        }
                    }
                }
                
                stage('Smoke Test') {
                    steps {
                        script {
                            // Run container and test basic functionality
                            docker.image("${env.FULL_IMAGE_TAG}").withRun('-p 5000:5000') { c ->
                                sh 'sleep 10' // Wait for app to start
                                sh '''
                                    curl -f http://localhost:5000/health || \\
                                    curl -f http://localhost:5000/ || \\
                                    echo "Health check endpoint not available"
                                '''
                            }
                        }
                    }
                }
            }
        }
        
        stage('Push to Registry') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                    expression { return params.CUSTOM_TAG != 'latest' }
                }
            }
            steps {
                script {
                    docker.withRegistry("https://${DOCKER_REGISTRY}", 'docker-registry-credentials') {
                        docker.image("${env.FULL_IMAGE_TAG}").push()
                        
                        // Also push with latest tag if on main branch
                        if (env.BRANCH_NAME == 'main') {
                            docker.image("${env.FULL_IMAGE_TAG}").push('latest')
                        }
                    }
                }
            }
        }
        
        stage('Deploy') {
            when {
                branch 'main'
            }
            steps {
                script {
                    // Example deployment to different environments based on Python version
                    def deploymentEnv = params.PYTHON_VERSION == '3.12' ? 'production' : 'staging'
                    
                    echo "Deploying to ${deploymentEnv} environment"
                    
                    // Update deployment manifest or trigger deployment
                    sh """
                        # Example: Update Kubernetes deployment
                        kubectl set image deployment/flask-app-${deploymentEnv} \\
                            flask-app=${env.FULL_IMAGE_TAG} \\
                            --namespace=${deploymentEnv} || echo "Deployment update failed"
                        
                        # Or update Docker Compose
                        # sed -i 's|image: .*flask-app.*|image: ${env.FULL_IMAGE_TAG}|' docker-compose.${deploymentEnv}.yml
                        # docker-compose -f docker-compose.${deploymentEnv}.yml up -d
                    """
                }
            }
        }
    }
    
    post {
        always {
            // Clean up
            sh '''
                docker image prune -f
                rm -f Dockerfile.generated
            '''
        }
        success {
            echo "Pipeline completed successfully with Python ${params.PYTHON_VERSION}!"
            // Send notification
            emailext(
                subject: "Flask App Build Success - Python ${params.PYTHON_VERSION}",
                body: "Build completed successfully with image: ${env.FULL_IMAGE_TAG}",
                to: "${env.CHANGE_AUTHOR_EMAIL}"
            )
        }
        failure {
            echo "Pipeline failed!"
            // Send failure notification
            emailext(
                subject: "Flask App Build Failed - Python ${params.PYTHON_VERSION}",
                body: "Build failed for Python ${params.PYTHON_VERSION}. Check Jenkins logs.",
                to: "${env.CHANGE_AUTHOR_EMAIL}"
            )
        }
    }
}
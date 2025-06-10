pipeline {
    agent any
    
    // Specify which branches to build
    options {
        // Only keep 10 builds
        buildDiscarder(logRotator(numToKeepStr: '10'))
        // Timeout the build after 1 hour
        timeout(time: 1, unit: 'HOURS')
    }
    
    // Define branch-specific triggers
    triggers {
        // Poll SCM for changes on main/master branch every 5 minutes
        pollSCM(env.BRANCH_NAME == 'main' ? 'H/5 * * * *' : '')
    }
    
    environment {
        // Define environment variables
        NODE_VERSION = '18'
        BUILD_ENV = getBuildEnvironment()
    }
    
    stages {
        stage('Checkout') {
            steps {
                // Checkout specific branch
                checkout scm
                script {
                    echo "Building branch: ${env.BRANCH_NAME}"
                    echo "Build environment: ${env.BUILD_ENV}"
                }
            }
        }
        
        stage('Setup') {
            steps {
                script {
                    // Branch-specific setup
                    if (env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'master') {
                        echo "Setting up production environment"
                    } else if (env.BRANCH_NAME.startsWith('develop')) {
                        echo "Setting up development environment"
                    } else if (env.BRANCH_NAME.startsWith('feature/')) {
                        echo "Setting up feature branch environment"
                    } else {
                        echo "Setting up default environment"
                    }
                }
            }
        }
        
        stage('Build') {
            steps {
                script {
                    // Example build commands - customize for your project
                    sh '''
                        echo "Building application..."
                        # Add your build commands here
                        # npm install
                        # npm run build
                        # mvn clean package
                        # docker build -t myapp:${BUILD_NUMBER} .
                    '''
                }
            }
        }
        
        stage('Test') {
            parallel {
                stage('Unit Tests') {
                    steps {
                        script {
                            sh '''
                                echo "Running unit tests..."
                                # Add your test commands here
                                # npm test
                                # mvn test
                            '''
                        }
                    }
                    post {
                        always {
                            // Publish test results if they exist
                            // publishTestResults testResultsPattern: 'test-results/*.xml'
                            echo "Unit tests completed"
                        }
                    }
                }
                stage('Integration Tests') {
                    when {
                        anyOf {
                            branch 'main'
                            branch 'master'
                            branch 'develop'
                        }
                    }
                    steps {
                        script {
                            sh '''
                                echo "Running integration tests..."
                                # Add integration test commands here
                            '''
                        }
                    }
                }
            }
        }
        
        stage('Deploy to Dev') {
            when {
                anyOf {
                    branch 'develop'
                    branch 'feature/*'
                }
            }
            steps {
                script {
                    echo "Deploying to development environment..."
                    // Add deployment commands for dev environment
                }
            }
        }
        
        stage('Deploy to Staging') {
            when {
                branch 'main'
            }
            steps {
                script {
                    echo "Deploying to staging environment..."
                    // Add deployment commands for staging environment
                }
            }
        }
        
        stage('Deploy to Production') {
            when {
                allOf {
                    branch 'main'
                    // Only deploy to prod if tests passed
                    expression { currentBuild.result != 'FAILURE' }
                }
            }
            steps {
                script {
                    // Require manual approval for production deployment
                    input message: 'Deploy to production?', ok: 'Deploy'
                    echo "Deploying to production environment..."
                    // Add deployment commands for production environment
                }
            }
        }
    }
    
    post {
        always {
            script {
                echo "Pipeline completed for branch: ${env.BRANCH_NAME}"
                // Clean up workspace
                cleanWs()
            }
        }
        success {
            script {
                if (env.BRANCH_NAME == 'main') {
                    echo "Main branch build successful - notifying team"
                    // Add notification logic (email, Slack, etc.)
                }
            }
        }
        failure {
            script {
                echo "Build failed for branch: ${env.BRANCH_NAME}"
                // Add failure notification logic
            }
        }
    }
}

// Helper function to determine build environment based on branch
def getBuildEnvironment() {
    switch(env.BRANCH_NAME) {
        case 'main':
        case 'master':
            return 'production'
        case 'develop':
            return 'development'
        case ~/^feature\/.*/:
            return 'feature'
        case ~/^hotfix\/.*/:
            return 'hotfix'
        default:
            return 'default'
    }
}
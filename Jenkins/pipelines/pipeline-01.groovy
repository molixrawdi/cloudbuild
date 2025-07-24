pipeline {
    agent any
    
    parameters {
        choice(
            name: 'PYTHON_VERSION',
            choices: ['3.9', '3.10', '3.11', '3.12'],
            description: 'Python version'
        )
        choice(
            name: 'BASE_IMAGE',
            choices: ['python', 'python-slim', 'python-alpine'],
            description: 'Base image type'  
        )
    }
    
    environment {
        IMAGE_NAME = "flask-app"
        BUILD_DATE = sh(script: "date -u +'%Y-%m-%dT%H:%M:%SZ'", returnStdout: true).trim()
        VERSION = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
    }
    
    stages {
        stage('Build') {
            steps {
                script {
                    def imageTag = "${IMAGE_NAME}:${params.PYTHON_VERSION}-${VERSION}"
                    
                    sh """
                        docker build \\
                            --build-arg PYTHON_VERSION=${params.PYTHON_VERSION} \\
                            --build-arg BASE_IMAGE=${params.BASE_IMAGE} \\
                            --build-arg BUILD_DATE=${BUILD_DATE} \\
                            --build-arg VERSION=${VERSION} \\
                            -t ${imageTag} \\
                            .
                    """
                    
                    env.BUILT_IMAGE = imageTag
                }
            }
        }
        
        stage('Test') {
            steps {
                sh """
                    docker run --rm ${env.BUILT_IMAGE} python -m pytest tests/ -v
                """
            }
        }
        
        stage('Push') {
            when { branch 'main' }
            steps {
                sh """
                    docker push ${env.BUILT_IMAGE}
                """
            }
        }
    }
}
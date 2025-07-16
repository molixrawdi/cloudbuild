pipeline {
    agent any

    environment {
        REPO = 'https://github.com/lvthillo/python-flask-docker.git'
        REPO_DIR = 'python-flask-docker'
        DOCKER_USER = 'tazmania'
        IMAGE_NAME = 'ludge'
    }

    stages {
        stage('Clone Repo') {
            steps {
                sh "git clone ${REPO}"
                sh "ls -la ${REPO_DIR}"
            }
        }

        stage('Get Short SHA') {
            steps {
                script {
                    dir("${REPO_DIR}") {
                        def shortSha = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
                        env.SHORT_SHA = shortSha
                        echo "Using short SHA: ${shortSha}"
                    }
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                dir("${REPO_DIR}") {
                    sh "docker build -t ${DOCKER_USER}/${IMAGE_NAME}:${SHORT_SHA} ."
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                sh "docker push ${DOCKER_USER}/${IMAGE_NAME}:${SHORT_SHA}"
            }
        }
    }
}

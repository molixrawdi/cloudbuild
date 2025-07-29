pipelineJob('my-pipeline-job') {
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('https://github.com/example/repo.git')
                    }
                    branches('main')
                }
            }
            scriptPath('Jenkinsfile')
        }
    }
}
pipeline {
    agent any
    environment {
        DOCKER_IMAGE = 'my-docker-image'
        FULL_IMAGE_TAG = "${DOCKER_IMAGE}:${env.BUILD_ID}"
    }
    
    stages {
        stage('Load Pipeline Config') {
            steps {
                script {
                    def pipelineConfig = loadPipelineConfig('/opt/jenkins/pipeline_configs')
                    echo "Loaded configuration: ${pipelineConfig}"
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    echo "Building Docker image ${FULL_IMAGE_TAG}"
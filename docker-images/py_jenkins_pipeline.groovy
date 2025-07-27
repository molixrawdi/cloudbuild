// Jenkinsfile
pipeline {
    agent any
    environment {
        PIPELINE_CONFIG_ROOT = '/opt/jenkins/pipeline_configs'
        JENKINS_HOME = '/var/lib/jenkins'
    }
    stages {
        stage('Load Config') {
            steps {
                script {
                    sh 'python3 load_pipeline_config.py'
                }
            }
        }
    }
}
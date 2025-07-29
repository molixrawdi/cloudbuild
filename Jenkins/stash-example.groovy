pipeline {
    agent none
    stages {
        stage('Build') {
            agent any
            steps {
                // Build and stash artifacts
                sh 'make build'
                stash name: 'backend-sha', includes: 'dist/**,package.json'
            }
        }
        stage('Deploy') {
            agent { label 'deployment-server' }
            steps {
                // Retrieve the built artifacts
                unstash 'backend-sha'
                sh 'deploy.sh'
            }
        }
    }
}

    post {
        always {
            echo 'Cleaning up...'
            // Clean up any resources if necessary
        }
    }
}
job('example-job') {
    description('A simple example job')
    scm {
        git('https://github.com/example/repo.git')
    }
    triggers {
        scm('H/15 * * * *') // Poll every 15 minutes
    }
    steps {
        shell('echo "Hello, World!"')
    }
}
                    sh 'docker build -t ${FULL_IMAGE_TAG} .'
                }
            }
        }
        
        stage('Push Docker Image') {
            steps {
                script {
                    sh 'docker push ${FULL_IMAGE_TAG}'
                }
            }
        }
        
        stage('Deploy') {
            steps {
                script {
                    echo "Deploying ${DOCKER_IMAGE} to staging environment"
                    // Add deployment logic here
                }
            }
        }
    }
    
    post {
        always {
            echo 'Cleaning up...'
            sh 'docker rmi ${FULL_IMAGE_TAG} || true'
        }
    }
}
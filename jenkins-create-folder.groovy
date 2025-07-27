// create_folders.groovy
folder('pipelines') {
    description('Main pipelines configuration folder')
    displayName('Pipeline Configurations')
}

folder('pipelines/microservices') {
    description('Microservices pipeline configurations')
}

folder('pipelines/deployment') {
    description('Deployment pipeline configurations')
}

folder('pipelines/testing') {
    description('Testing pipeline configurations')
}

// Create jobs within folders
pipelineJob('pipelines/microservices/user-service') {
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('https://github.com/company/user-service.git')
                    }
                    branch('*/main')
                }
            }
            scriptPath('Jenkinsfile')
        }
    }
}
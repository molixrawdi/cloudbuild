// vars/loadPipelineConfig.groovy
def call(String configPath = null) {
    def defaultConfigPath = "${env.JENKINS_HOME}/pipeline-configs"
    def actualConfigPath = configPath ?: defaultConfigPath
    
    // Ensure config directory exists
    sh "mkdir -p ${actualConfigPath}"
    
    // Load configuration based on job name or branch
    def configFile = "${actualConfigPath}/${env.JOB_NAME}.yaml"
    
    if (fileExists(configFile)) {
        return readYaml file: configFile
    } else {
        // Return default configuration
        return [
            build: [timeout: 30, agent: 'any'],
            deploy: [environment: 'staging'],
            notifications: [email: true]
        ]
    }
}

// Usage in Jenkinsfile
pipeline {
    agent any
    stages {
        stage('Load Config') {
            steps {
                script {
                    def pipelineConfig = loadPipelineConfig('/opt/jenkins/custom-configs')
                    echo "Loaded configuration: ${pipelineConfig}"
                }
            }
        }
    }
}
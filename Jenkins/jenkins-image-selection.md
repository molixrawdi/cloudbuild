// Method 1: Using Active Choices Plugin with Docker Registry API
pipeline {
    agent any
    
    parameters {
        // Static fallback choice
        choice(
            name: 'FALLBACK_IMAGE',
            choices: ['latest', 'stable', 'dev'],
            description: 'Fallback if dynamic loading fails'
        )
        
        // Dynamic image selection using Active Choices Plugin
        activeChoice(
            name: 'DOCKER_IMAGE',
            description: 'Select Docker image from registry',
            filterable: true,
            choiceType: 'SINGLE_SELECT',
            script: [
                $class: 'GroovyScript',
                sandbox: false,
                script: '''
                    import groovy.json.JsonSlurper
                    import java.net.http.*
                    import java.net.URI
                    
                    def getDockerHubTags(repository) {
                        try {
                            def url = "https://registry.hub.docker.com/v2/repositories/${repository}/tags?page_size=50"
                            def response = new URL(url).getText()
                            def json = new JsonSlurper().parseText(response)
                            
                            def tags = json.results.collect { "${repository}:${it.name}" }
                            return tags.take(20) // Limit to 20 most recent
                        } catch (Exception e) {
                            return ["${repository}:latest", "${repository}:stable"]
                        }
                    }
                    
                    def getPrivateRegistryTags(registry, repository, token = null) {
                        try {
                            def url = "https://${registry}/v2/${repository}/tags/list"
                            def connection = new URL(url).openConnection()
                            
                            if (token) {
                                connection.setRequestProperty("Authorization", "Bearer ${token}")
                            }
                            
                            def response = connection.getInputStream().getText()
                            def json = new JsonSlurper().parseText(response)
                            
                            return json.tags.collect { "${registry}/${repository}:${it}" }.take(20)
                        } catch (Exception e) {
                            return ["${registry}/${repository}:latest"]
                        }
                    }
                    
                    // Configure your registry and repository here
                    def repository = "library/python"  // For Docker Hub
                    // def registry = "your-registry.com"
                    // def repository = "your-org/your-app"
                    
                    return getDockerHubTags(repository)
                '''
            ]
        )
        
        // Cascade choice for tags based on selected image
        cascadeChoiceParameter(
            name: 'IMAGE_TAG',
            description: 'Select specific tag',
            referencedParameters: 'DOCKER_IMAGE',
            choiceType: 'SINGLE_SELECT',
            script: [
                $class: 'GroovyScript',
                sandbox: false,
                script: '''
                    import groovy.json.JsonSlurper
                    
                    def getTagsForImage(imageWithTag) {
                        try {
                            def parts = imageWithTag.split(':')
                            def imageName = parts[0]
                            
                            if (imageName.startsWith('library/')) {
                                // Docker Hub official image
                                def repo = imageName.substring(8) // Remove 'library/'
                                def url = "https://registry.hub.docker.com/v2/repositories/library/${repo}/tags?page_size=30"
                            } else {
                                // Regular Docker Hub image
                                def url = "https://registry.hub.docker.com/v2/repositories/${imageName}/tags?page_size=30"
                            }
                            
                            def response = new URL(url).getText()
                            def json = new JsonSlurper().parseText(response)
                            
                            return json.results.collect { it.name }.take(15)
                        } catch (Exception e) {
                            return ['latest', 'stable', '3.11', '3.10', '3.9']
                        }
                    }
                    
                    if (!DOCKER_IMAGE) {
                        return ['latest']
                    }
                    
                    return getTagsForImage(DOCKER_IMAGE)
                '''
            ]
        )
    }
    
    stages {
        stage('Show Selected Image') {
            steps {
                script {
                    echo "Selected Docker Image: ${params.DOCKER_IMAGE}"
                    echo "Selected Tag: ${params.IMAGE_TAG}"
                    
                    // Construct final image name
                    def finalImage = "${params.DOCKER_IMAGE.split(':')[0]}:${params.IMAGE_TAG}"
                    env.FINAL_IMAGE = finalImage
                    echo "Final Image: ${env.FINAL_IMAGE}"
                }
            }
        }
        
        stage('Validate Image') {
            steps {
                script {
                    // Verify image exists
                    sh "docker pull ${env.FINAL_IMAGE}"
                    sh "docker inspect ${env.FINAL_IMAGE}"
                }
            }
        }
    }
}

// Method 2: Pre-build step to populate choices (using build parameters)
@Library('your-shared-library') _

pipeline {
    agent any
    
    stages {
        stage('Get Available Images') {
            when {
                // Only run this when we need to refresh the image list
                expression { params.REFRESH_IMAGE_LIST == true }
            }
            steps {
                script {
                    def images = getAvailableImages()
                    
                    // Trigger a new build with updated parameters
                    build job: env.JOB_NAME, parameters: [
                        string(name: 'AVAILABLE_IMAGES', value: images.join(',')),
                        booleanParam(name: 'REFRESH_IMAGE_LIST', value: false)
                    ], wait: false
                    
                    // Stop current build
                    currentBuild.result = 'ABORTED'
                    error('Refreshing image list. New build started with updated images.')
                }
            }
        }
        
        stage('Build with Selected Image') {
            steps {
                script {
                    echo "Using image: ${params.SELECTED_IMAGE}"
                    sh "docker run --rm ${params.SELECTED_IMAGE} python --version"
                }
            }
        }
    }
}

// Method 3: Using Extended Choice Parameter Plugin
pipeline {
    agent any
    
    parameters {
        extendedChoice(
            name: 'DOCKER_IMAGES',
            description: 'Select Docker images (multiple selection)',
            type: 'PT_CHECKBOX',
            value: getImageList(), // This calls a global function
            defaultValue: 'python:3.11,python:3.10',
            multiSelectDelimiter: ','
        )
    }
    
    stages {
        stage('Process Selected Images') {
            steps {
                script {
                    def selectedImages = params.DOCKER_IMAGES.split(',')
                    
                    selectedImages.each { image ->
                        echo "Processing image: ${image}"
                        sh "docker pull ${image}"
                    }
                }
            }
        }
    }
}

// Global function for Extended Choice (put in shared library)
def getImageList() {
    try {
        def registryUrl = "https://your-registry.com/v2"
        def repository = "your-org/your-app"
        
        // Using curl to get tags
        def curlCommand = """
            curl -s -H "Authorization: Bearer \$(curl -s 'https://auth.docker.io/token?service=registry.docker.io&scope=repository:${repository}:pull' | jq -r .token)" \\
            '${registryUrl}/${repository}/tags/list' | jq -r '.tags[]'
        """.stripIndent()
        
        def process = curlCommand.execute()
        def output = process.text.trim()
        
        if (process.exitValue() == 0) {
            def tags = output.split('\n').collect { "${repository}:${it}" }
            return tags.join(',')
        } else {
            // Fallback images
            return "python:3.11,python:3.10,python:3.9,python:latest"
        }
    } catch (Exception e) {
        // Fallback images
        return "python:3.11,python:3.10,python:3.9,python:latest"
    }
}

// Method 4: Using Jenkins Shared Library with Registry API
@Library('docker-registry-lib') _

pipeline {
    agent any
    
    parameters {
        activeChoice(
            name: 'REGISTRY_TYPE',
            description: 'Select registry type',
            choiceType: 'SINGLE_SELECT',
            script: [
                $class: 'GroovyScript',
                sandbox: false,
                script: 'return ["Docker Hub", "AWS ECR", "Google GCR", "Azure ACR", "Harbor", "Custom Registry"]'
            ]
        )
        
        cascadeChoiceParameter(
            name: 'AVAILABLE_IMAGES',
            description: 'Available images from selected registry',
            referencedParameters: 'REGISTRY_TYPE',
            choiceType: 'SINGLE_SELECT',
            script: [
                $class: 'GroovyScript',
                sandbox: false,
                script: '''
                    def getImagesFromRegistry(registryType) {
                        switch(registryType) {
                            case 'Docker Hub':
                                return getDockerHubImages('your-org')
                            case 'AWS ECR':
                                return getECRImages('your-account-id', 'us-east-1')
                            case 'Google GCR':
                                return getGCRImages('your-project-id')
                            case 'Azure ACR':
                                return getACRImages('your-registry.azurecr.io')
                            case 'Harbor':
                                return getHarborImages('your-harbor-url.com')
                            default:
                                return ['custom-image:latest']
                        }
                    }
                    
                    def getDockerHubImages(organization) {
                        try {
                            def url = "https://registry.hub.docker.com/v2/repositories/${organization}/?page_size=25"
                            def response = new URL(url).getText()
                            def json = new groovy.json.JsonSlurper().parseText(response)
                            
                            return json.results.collect { "${organization}/${it.name}:latest" }
                        } catch (Exception e) {
                            return ["${organization}/app:latest"]
                        }
                    }
                    
                    def getECRImages(accountId, region) {
                        try {
                            // This would require AWS CLI configured in Jenkins
                            def command = "aws ecr describe-repositories --region ${region} --query 'repositories[].repositoryName' --output text"
                            def process = command.execute()
                            def repos = process.text.trim().split('\\s+')
                            
                            return repos.collect { "${accountId}.dkr.ecr.${region}.amazonaws.com/${it}:latest" }
                        } catch (Exception e) {
                            return ["${accountId}.dkr.ecr.${region}.amazonaws.com/app:latest"]
                        }
                    }
                    
                    def getGCRImages(projectId) {
                        try {
                            // This would require gcloud configured in Jenkins
                            def command = "gcloud container images list --repository=gcr.io/${projectId} --format='value(name)'"
                            def process = command.execute()
                            def images = process.text.trim().split('\n')
                            
                            return images.collect { "${it}:latest" }
                        } catch (Exception e) {
                            return ["gcr.io/${projectId}/app:latest"]
                        }
                    }
                    
                    if (!REGISTRY_TYPE) {
                        return ['No registry selected']
                    }
                    
                    return getImagesFromRegistry(REGISTRY_TYPE)
                '''
            ]
        )
    }
    
    environment {
        SELECTED_IMAGE = "${params.AVAILABLE_IMAGES}"
    }
    
    stages {
        stage('Use Selected Image') {
            steps {
                script {
                    echo "Registry Type: ${params.REGISTRY_TYPE}"
                    echo "Selected Image: ${env.SELECTED_IMAGE}"
                    
                    // Authenticate and pull image based on registry type
                    switch(params.REGISTRY_TYPE) {
                        case 'AWS ECR':
                            sh '''
                                aws ecr get-login-password --region us-east-1 | \\
                                docker login --username AWS --password-stdin ${SELECTED_IMAGE.split('/')[0]}
                            '''
                            break
                        case 'Google GCR':
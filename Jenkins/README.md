# These are the most important plugings I installed on test Jenkins


Preparation	
Checking internet connectivity
Checking update center connectivity
Success
Cloud Statistics	 Success
Docker	 Success
Kubernetes Client API	 Success
Kubernetes Credentials	 Success
Kubernetes	 Success
Node Iterator API	 Success
Mina SSHD API :: SCP	 Success
Amazon Web Services SDK 2 :: Core	 Success
Amazon Web Services SDK 2 :: EC2	 Success
Amazon Web Services SDK :: Minimal	 Success
Amazon Web Services SDK :: EC2	 Success
AWS Credentials	 Success
Amazon EC2	 Success
Netty API	 Success
Azure SDK API	 Success
Azure Credentials	 Success
Azure VM Agents	 Success
Icon Shim	 Success
Yet Another Docker	 Success
JClouds	 Success
Mac	 Success
Nomad	 Success
Amazon Web Services SDK :: SQS	 Success
Amazon Web Services SDK :: SNS	 Success
Amazon Web Services SDK :: Api Gateway	 Success
Amazon Web Services SDK :: Autoscaling	 Success
Amazon Web Services SDK :: CloudFormation	 Success
Amazon Web Services SDK :: Elastic Beanstalk	 Success
Amazon Web Services SDK :: Elastic Load Balancing V2	 Success
Amazon Web Services SDK :: ECS	 Success
Amazon Web Services SDK :: IAM	 Success
Amazon Web Services SDK :: ECR	 Success
Amazon Web Services SDK :: EFS	 Success
Amazon Web Services SDK :: CloudWatch	 Success
Amazon Web Services SDK :: CloudFront	 Success
Amazon Web Services SDK :: SSM	 Success
Amazon Web Services SDK :: kinesis	 Success
Amazon Web Services SDK :: Logs	 Success
Amazon Web Services SDK :: Lambda	 Success
Amazon Web Services SDK :: CodeBuild	 Success
Amazon Web Services SDK :: Organizations	 Success
Amazon Web Services SDK :: CodeDeploy	 Success
Amazon Web Services SDK :: Secrets Manager	 Success
Amazon Web Services SDK :: All	 Success
AWS Lambda Cloud	 Success
Libvirt Agents	 Success
AWS Codebuild Cloud	 Success
Loading plugin extensions	 Success
Go back to the top pages


### To install the plugins using the cli

```

java -jar jenkin-cli.jar -s http://<Target-Jenkins-server-url/> install-plugin parameterized-trigger <PLUGIN-1-URL> <PlUGIN-2-URL> --username <USER-NAME> --password <PASSWORD> --restart

```
The above can be put in a shell script and parameters passed in this way:

source script-name param-02 param-02 param-03

There is a command that can help list the plugins:

```
java -jar jenkins-cli.jar -s http://<Jenkins-server-url-or-ip> list-plugins --username <USER-NAME> --password <PASSWORD>
```

Select the plugin needed from:

https://updates.jenkins-ci.org/downloads/plugins


## Agents

Jenkins pipelines can have several types of agents that determine where and how your pipeline stages execute:
Agent Types
any - Runs on any available agent in the Jenkins environment. This is the most flexible option but gives you no control over the execution environment.
none - No global agent is allocated for the pipeline. Each stage must define its own agent. This is useful when different stages need different execution environments.
label - Runs on agents with specific labels. You can target agents based on their capabilities, operating system, or other characteristics:


```
agent { label 'linux && docker' }
```

### node

```
agent { 
    node {
        label 'windows'
        customWorkspace '/custom/path'
    }
}
```
### Docker

```

agent { 
    docker {
        image 'maven:3.8.1-adoptopenjdk-11'
        args '-v /tmp:/tmp'
    }
}
```

### Dockerfile

```
agent {
    dockerfile {
        filename 'Dockerfile.build'
        dir 'build'
    }
}
```

### Kubernetes
```
agent {
    kubernetes {
        yaml '''
        spec:
          containers:
          - name: maven
            image: maven:3.8.1-adoptopenjdk-11
        '''
    }
}

```
Pipeline Structure Options
pipeline - The root block that defines a declarative pipeline
agent - Specifies where the pipeline or stage will run

any, none, label, node, docker, dockerfile, kubernetes

stages - Contains all the pipeline stages
stage - Individual stage within the pipeline
steps - Contains the actual build steps within a stage
Pipeline Directives
environment - Defines environment variables

```
environment {
    CC = 'clang'
    PATH = "$PATH:/usr/local/bin"
}
```

### parameters:

```
parameters {
    string(name: 'BRANCH', defaultValue: 'main')
    booleanParam(name: 'DEPLOY', defaultValue: false)
    choice(name: 'ENVIRONMENT', choices: ['dev', 'staging', 'prod'])
}

```
### Triggers

```
triggers {
    cron('H 2 * * *')
    pollSCM('H/5 * * * *')
    upstream(upstreamProjects: 'job1,job2', threshold: hudson.model.Result.SUCCESS)
}
```
### Options
```
options {
    buildDiscarder(logRotator(numToKeepStr: '10'))
    timeout(time: 1, unit: 'HOURS')
    retry(3)
    skipDefaultCheckout()
    skipStagesAfterUnstable()
    checkoutToSubdirectory('src')
    newContainerPerStage()
    preserveStashes()
    quietPeriod(5)
    disableConcurrentBuilds()
    parallelsAlwaysFailFast()
}
```

### Tools

```
tools {
    maven 'maven-3.8.1'
    jdk 'jdk-11'
    nodejs 'node-16'
}
```
### Inputs


```
input {
    message "Deploy to production?"
    ok "Deploy"
    submitter "admin,deploy-team"
    parameters {
        choice(name: 'TARGET', choices: ['staging', 'production'])
    }
}
```
### When / Conditions

```
when {
    branch 'main'
    environment name: 'DEPLOY', value: 'true'
    not { branch 'PR-*' }
    anyOf {
        branch 'main'
        branch 'develop'
    }
    allOf {
        branch 'main'
        environment name: 'DEPLOY', value: 'true'
    }
    buildingTag()
    changelog '.*\\[ci skip\\].*'
    changeset "**/*.js"
    changeRequest()
    equals expected: 2, actual: currentBuild.number
    expression { return params.ENVIRONMENT == 'production' }
    tag "release-*"
    triggeredBy 'SCMTrigger'
}
```

### Stage level 


```
parallel {
    stage('Test A') { /* ... */ }
    stage('Test B') { /* ... */ }
}
```

### Matrix

```
matrix {
    axes {
        axis {
            name 'PLATFORM'
            values 'linux', 'windows', 'mac'
        }
    }
    stages {
        stage('test') { /* ... */ }
    }
}
```

### Post

```

post {
    always { /* runs regardless of build result */ }
    success { /* runs only if successful */ }
    failure { /* runs only if failed */ }
    unstable { /* runs only if unstable */ }
    changed { /* runs only if status changed */ }
    fixed { /* runs only if previous build failed and current succeeded */ }
    regression { /* runs only if previous build succeeded and current failed */ }
    aborted { /* runs only if aborted */ }
    unsuccessful { /* runs if not successful */ }
    cleanup { /* runs after all other post conditions */ }
}

```

### Library

```

libraries {
    lib('my-shared-library@main')
}
```


### Custom tricks increase visibility

#### Show shots sha in pipeline

##### Using email extension macros
```
pipeline {
    agent any
    
    post {
        always {
            emailext (
                subject: "${env.JOB_NAME} - Build #${env.BUILD_NUMBER} - ${currentBuild.currentResult} - ${env.GIT_COMMIT[0..7]}",
                body: "Build ${env.BUILD_NUMBER} completed with status: ${currentBuild.currentResult}",
                to: "team@example.com"
            )
        }
    }
}
```


##### Using Git Token Macros

```
pipeline {
    agent any
    
    post {
        always {
            emailext (
                subject: '$PROJECT_NAME - Build #$BUILD_NUMBER - $BUILD_STATUS - ${GIT_REVISION,length=8}',
                body: '$DEFAULT_CONTENT',
                to: '$DEFAULT_RECIPIENTS'
            )
        }
    }
}
```


Method 3: Global Configuration
You can also modify the default subject globally in Jenkins:

Go to Manage Jenkins → Configure System
Find the Extended E-mail Notification section
Modify the Default Subject field to include:
$PROJECT_NAME - Build #$BUILD_NUMBER - $BUILD_STATUS - ${GIT_REVISION,length=8}


Available Git-Related Token Macros

${GIT_REVISION} - Full commit SHA
${GIT_REVISION,length=8} - Short commit SHA (first 8 characters)
${GIT_BRANCH} - Git branch name
${GIT_AUTHOR_NAME} - Author name
${GIT_AUTHOR_EMAIL} - Author email


##### More Details

```
pipeline {
    agent any
    
    post {
        failure {
            emailext (
                subject: "FAILED: ${env.JOB_NAME} - Build #${env.BUILD_NUMBER} - ${env.GIT_COMMIT[0..7]}",
                body: """
                Build Failed!
                Job: ${env.JOB_NAME}
                Build Number: ${env.BUILD_NUMBER}
                Git Commit: ${env.GIT_COMMIT}
                Git Branch: ${env.GIT_BRANCH}
                """,
                to: "team@example.com"
            )
        }
        success {
            emailext (
                subject: "SUCCESS: ${env.JOB_NAME} - Build #${env.BUILD_NUMBER} - ${env.GIT_COMMIT[0..7]}",
                body: "Build completed successfully!",
                to: "team@example.com"
            )
        }
    }
}
```

##### Note:
The ${env.GIT_COMMIT[0..7]} syntax extracts the first 8 characters of the commit SHA, giving you the short SHA that's commonly used in Git operations.

To generate requirements.txt use pip freeze
```
pip freeze > requirements.txt
```
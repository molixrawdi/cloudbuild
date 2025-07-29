### make agent node-01

```
job('freestyle-on-node-01') {
    label('node-01') // This tells Jenkins to run the job on node-01
    steps {
        shell('echo "Running on node-01"')
    }
}

```

### script from scm

```
pipelineJob('pipeline-on-node-01') {
    description('Pipeline job restricted to node-01')
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('https://github.com/example/repo.git')
                    }
                    branches('*/main')
                }
            }
            scriptPath('Jenkinsfile')
        }
    }
    // This sets a label restriction for agent selection
    properties {
        label('node-01')
    }
}

```

### jenkins file declare

```
pipeline {
    agent { label 'node-01' }
    stages {
        stage('Run') {
            steps {
                echo 'Running on node-01'
            }
        }
    }
}

```


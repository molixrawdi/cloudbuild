

'''
// Single file
jobDsl targets: 'jobs/my-job.groovy'

// Multiple specific files
jobDsl targets: 'jobs/job1.groovy,jobs/job2.groovy'

// Different directory patterns
jobDsl targets: 'jenkins/**/*.groovy'
jobDsl targets: 'jobs/*.groovy'
jobDsl targets: 'dsl-scripts/**/*.groovy'

'''

     ### 2nd example below

'''
// Remove existing jobs not defined in DSL
jobDsl targets: 'dsljobs/**/*.groovy',
       removedJobAction: 'DELETE'

// Ignore existing jobs
jobDsl targets: 'dsljobs/**/*.groovy',
       removedJobAction: 'IGNORE'

// Disable removed jobs instead of deleting
jobDsl targets: 'dsljobs/**/*.groovy',
       removedJobAction: 'DISABLE'

'''

### 3rd example below

'''
jobDsl scriptText: '''
    job('my-generated-job') {
        steps {
            shell('echo "Hello World"')
        }
    }
'''

### 4th example below

'''
jobDsl targets: 'dsljobs/**/*.groovy',
       additionalParameters: [
           environment: 'prod',
           version: '1.2.3'
       ]

'''

### 5th example below

'''
jobDsl targets: 'dsljobs/**/*.groovy',
       removedJobAction: 'DELETE',
       removedViewAction: 'DELETE',
       lookupStrategy: 'JENKINS_ROOT',
       additionalClasspath: 'lib/**/*.jar',
       failOnMissingPlugin: true,
       unstableOnDeprecation: true

'''

### 6th

'''
script {
    def dslPattern = env.BRANCH_NAME == 'main' ? 'dsljobs/prod/**/*.groovy' : 'dsljobs/dev/**/*.groovy'
    jobDsl targets: dslPattern
}
'''

### Link: https://jenkins.io/doc/pipeline/steps/job-dsl/

#### Sparce chekouts: Allo one to checkout certain paths from folders.

```

checkout([
    $class: 'GitSCM',
    branches: [[name: 'main']],
    extensions: [
        [$class: 'SparseCheckoutPaths', 
         sparseCheckoutPaths: [
             [$class: 'SparseCheckoutPath', path: 'src/'],
             [$class: 'SparseCheckoutPath', path: 'docs/'],
             [$class: 'SparseCheckoutPath', path: 'Dockerfile']
         ]]
    ],
    userRemoteConfigs: [[url: 'https://github.com/user/repo.git']]
])
```
### example:

```

pipeline {
    agent any
    stages {
        stage('Checkout') {
            steps {
                checkout scmGit(
                    branches: [[name: 'main']],
                    extensions: [
                        sparseCheckoutPaths([
                            sparseCheckoutPath('frontend/'),
                            sparseCheckoutPath('shared/'),
                            sparseCheckoutPath('package.json')
                        ])
                    ],
                    userRemoteConfigs: [[url: 'https://github.com/user/repo.git']]
                )
            }
        }
    }
}
```

### example

```

// Only specific directories
sparseCheckoutPaths: [
    [$class: 'SparseCheckoutPath', path: 'backend/'],
    [$class: 'SparseCheckoutPath', path: 'shared/']
]

// Exclude patterns (use ! prefix)
sparseCheckoutPaths: [
    [$class: 'SparseCheckoutPath', path: '*'],
    [$class: 'SparseCheckoutPath', path: '!tests/']
]

// Multiple specific files
sparseCheckoutPaths: [
    [$class: 'SparseCheckoutPath', path: 'Dockerfile'],
    [$class: 'SparseCheckoutPath', path: 'docker-compose.yml'],
    [$class: 'SparseCheckoutPath', path: 'package.json']
]
```
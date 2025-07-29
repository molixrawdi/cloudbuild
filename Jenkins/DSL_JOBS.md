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
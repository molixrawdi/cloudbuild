stage('TG full stack') {

    steps{
        script {
            unstash 'd-value'
            unstash 'd-value-2'
            deployStack(cfRepo: env.CF_REPO, 
                        cfBranch: env.CF_BRANCH, 
                        cfFile: 'docker-compose.yml', 
                        cfService: 'tg-full-stack', 
                        cfEnv: 'dev', 
                        cfVars: ['CF_VAR1=value1', 'CF_VAR2=value2'])       
            }
    }
}
job('build-with-params') {
    parameters {
        stringParam('BRANCH_NAME', 'main', 'Git branch to build')
        booleanParam('RUN_TESTS', true, 'Whether to run tests')
    }
    steps {
        shell('echo "Building branch: $BRANCH_NAME"')
        shell('if [ "$RUN_TESTS" = "true" ]; then echo "Running tests..."; fi')
    }
}
                            docker.image("${env.FULL_IMAGE_TAG}").push('latest')
                        }
                    }
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
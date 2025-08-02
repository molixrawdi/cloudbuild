class DockerImageLister {
    static List<String> listImages(String repo) {
        def cmd = "gcloud artifacts docker images list ${repo} --format='value(NAME)'"
        def proc = cmd.execute()
        proc.waitFor()
        if (proc.exitValue() == 0) {
            return proc.in.text.readLines()
        } else {
            return []
        }
    }
}

######

// Place in your pipeline script (requires Active Choices Plugin)
properties([
    [$class: 'ParametersDefinitionProperty', parameterDefinitions: [
        [$class: 'CascadeChoiceParameter',
            choiceType: 'PT_SINGLE_SELECT',
            name: 'DOCKER_IMAGE',
            description: 'Select Docker image from Artifact Registry',
            filterable: true,
            referencedParameters: '',
            script: [
                $class: 'GroovyScript',
                script: [
                    sandbox: false,
                    script: """
                        import DockerImageLister
                        return DockerImageLister.listImages('europe-west1-docker.pkg.dev/cortexica-atmscrts-int-d/atom-be')
                    """
                ]
            ]
        ]
    ]]
])




####

// Example substitution
static List<String> listImages() {
    return [
        "europe-west1-docker.pkg.dev/cortexica-atmscrts-int-d/atom-be/image1:latest",
        "europe-west1-docker.pkg.dev/cortexica-atmscrts-int-d/atom-be/image2:latest",
        "europe-west1-docker.pkg.dev/cortexica-atmscrts-int-d/atom-be/image3:latest"
    ]
}

#######

static List<String> listImages() {
    def cmd = "gcloud artifacts docker images list europe-west1-docker.pkg.dev/cortexica-atmscrts-int-d/atom-be --format='value(NAME)'"
    def proc = cmd.execute()
    proc.waitFor()
    if (proc.exitValue() == 0) {
        return proc.in.text.readLines()
    } else {
        return []
    }
}


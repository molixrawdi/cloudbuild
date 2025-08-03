unclassified:
  globalLibraries:
    libraries:
    - name: "my-shared-library"
      defaultVersion: "main"
      implicit: true
      allowVersionOverride: true
      includeInChangesets: true
      retriever:
        modernSCM:
          scm:
            git:
              remote: "https://github.com/your-org/jenkins-shared-library.git"
              credentialsId: "github-credentials"
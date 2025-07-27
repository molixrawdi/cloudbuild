# jenkins.yaml
jenkins:
  systemMessage: "Jenkins configured automatically by Jenkins Configuration as Code plugin"
  
jobs:
  - script: >
      folder('pipelines') {
        description('Main pipelines folder')
      }
      folder('pipelines/backend') {
        description('Backend service pipelines')
      }
      folder('pipelines/frontend') {
        description('Frontend application pipelines')
      }
      folder('pipelines/infrastructure') {
        description('Infrastructure and deployment pipelines')
      }
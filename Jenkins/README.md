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
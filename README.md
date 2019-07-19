# slackNotify
Jenkins pipeline library to extend Slack notifications

![notification](/images/notification.png)

## Usage:
### SetUp shared lib:

![setup](/images/setup.png)

### Jenkinsfile:
```
  @Library('slack')_
  pipeline {
    agent {
      any
    }
  }
  ... Some pipeline stages ...
  post {
   always {
     notifySlack(currentBuild.currentResult, '#jenkins')
  }
```

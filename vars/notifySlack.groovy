#!/usr/bin/env groovy

/**
* notify slack and set message based on build status
*/
import groovy.json.JsonOutput
import hudson.tasks.test.AbstractTestResultAction;
import hudson.model.Actionable;

def call(String buildStatus = 'STARTED', String channel = '#jenkins') {

  // buildStatus of null means successfull
  buildStatus = buildStatus ?: 'SUCCESSFUL'
  channel = channel ?: '#jenkins'
  user = 'jenkins'
  
  // Default values
  def colorCode = 'good'
  def subject = "${buildStatus}: Job '${env.JOB_BASE_NAME} [${env.BUILD_NUMBER}] (<${env.RUN_DISPLAY_URL}|Open>) (<${env.RUN_CHANGES_DISPLAY_URL}|  Changes>)'"
  def title = "${env.JOB_NAME} Build: ${env.BUILD_NUMBER}"
  def title_link = "${env.RUN_DISPLAY_URL}"
  def branchName = "${env.BRANCH_NAME}"

  def commit = sh(returnStdout: true, script: 'git rev-parse HEAD')
  def author = sh(returnStdout: true, script: "git --no-pager show -s --format='%an'").trim()

  def message = sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()

  // Override default values based on build status
  if (buildStatus == 'STARTED') {
    color = 'YELLOW'
    colorCode = '#FFFF00'
  } else if (buildStatus == 'SUCCESS') {
    color = 'GREEN'
    colorCode = 'good'
  } else if (buildStatus == 'UNSTABLE') {
    color = 'YELLOW'
    colorCode = 'warning'
  } else if (buildStatus == 'DEPLOY') {
    color = 'BLUE'
    colorCode = '#339CFF'
  } else {
    color = 'RED'
    colorCode = 'danger'
  }
  // get test results for slack message
  @NonCPS
  def getTestSummary = { ->
    def testResultAction = currentBuild.rawBuild.getAction(AbstractTestResultAction.class)
    def summary = ""

    if (testResultAction != null) {
        def total = testResultAction.getTotalCount()
        def failed = testResultAction.getFailCount()
        def skipped = testResultAction.getSkipCount()

        summary = "Test results:\n\t"
        summary = summary + ("Passed: " + (total - failed - skipped))
        summary = summary + (", Failed: " + failed + " ${testResultAction.failureDiffString}")
        summary = summary + (", Skipped: " + skipped)
    } else {
        summary = "No tests found"
    }
    return summary
  }
  def testSummaryRaw = getTestSummary()
  // format test summary as a code block
  def testSummary = "```${testSummaryRaw}```"
  println testSummary.toString()

  def attachments = [
    [
      author_name: 'jenkins',
      title: title.toString(),
      title_link: title_link.toString(),
      text: subject.toString(),
      fallback: 'fallback message',
      color: colorCode,
      fields:[
        [
          title: 'Branch',
          value: branchName.toString(),
          short: true
        ],
        [
          title: 'Author',
          value: author.toString(),
          short: true
        ],
        [
          title: 'Commit Message',
          value: message.toString(),
          short: false
        ],
        [
          title: 'Test Summary',
          value: testSummary.toString(),
          short: false
        ]
      ]
    ]
  ]
    
  // Send notifications
  slackSend(username: user, color: colorCode, channel: channel, attachments: JsonOutput.toJson(attachments))
}

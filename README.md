[![Build Status](https://dev.azure.com/datadoghq/jenkins-datadog-plugin/_apis/build/status/DataDog.jenkins-datadog-plugin?branchName=master)](https://dev.azure.com/datadoghq/jenkins-datadog-plugin/_build/latest?definitionId=18&branchName=master)

# Jenkins Datadog Plugin
A Jenkins plugin used to forward metrics, events, and service checks to an account at Datadog, automatically.

There is a [Jenkins-CI Wiki page](https://wiki.jenkins-ci.org/display/JENKINS/Datadog+Plugin) for this plugin, but it refers to our [DataDog/jenkins-datadog-plugin](https://github.com/DataDog/jenkins-datadog-plugin) for documentation.

## Features
Currently, the plugin is tracking the following data.

List of events:
* Started build
* Finished build
* SCM Checkout


| Metric Name              | Description                                                   |
|--------------------------|---------------------------------------------------------------|
| jenkins.executor.count   | Executor count                                                |
| jenkins.executor.free    | Number of unused executor                                     |
| jenkins.executor.in_use  | Number of idle executor                                       |
| jenkins.job.completed    | Rate of completed jobs                                        |
| jenkins.job.cycletime    | Build Cycle Time                                              |
| jenkins.job.duration     | Build duration (in seconds)                                   |
| jenkins.job.feedbacktime | Feedback time from code commit to job failure                 |
| jenkins.job.leadtime     | Build Lead Time                                               |
| jenkins.job.mtbf         | MTBF, time between last successful job and current failed job |
| jenkins.job.mttr         | MTTR: time between last failed job and current successful job |
| jenkins.job.started      | Rate of started jobs                                          |
| jenkins.job.waiting      | Time spent waiting for job to run (in milliseconds)           |
| jenkins.node.count       | Total number of node                                          |
| jenkins.node.offline     | Offline nodes count                                           |
| jenkins.node.online      | Online nodes count                                            |
| jenkins.plugin.count     | Plugins count                                                 |
| jenkins.project.count    | Project count                                                 |
| jenkins.queue.size       | Queue Size                                                    |
| jenkins.queue.buildable  | Number of Buildable item in Queue                             |
| jenkins.queue.pending    | Number of Pending item in Queue                               |
| jenkins.queue.stuck      | Number of Stuck item in Queue                                 |
| jenkins.queue.blocked    | Number of Blocked item in Queue                               |
| jenkins.scm.checkout     | Rate of SCM checkouts                                         |


List of service checks:
* Build status (jenkins.job.status)

All events, metrics, and service checks include the following tags, if they are available:
* `job`
* `result`
* (Git Branch, SVN revision or CVS branch) `branch` 
  * Git Branch available when using the [Git Plugin](https://wiki.jenkins.io/display/JENKINS/Git+Plugin)
* `node`

`jenkins.executor.*` metrics have the following additional tags:
* `node_hostname`
* `node_name`
* `node_label`


## Customization
From the global configuration page, at `Manage Jenkins -> Configure System`.
* Blacklisted Jobs
	* A comma-separated list of regex to match job names that should not be monitored. (eg: susans-job,johns-.*,prod_folder/prod_release).
* Whitelisted Jobs
	* A comma-separated list of regex to match job names that should be monitored. (eg: susans-job,johns-.*,prod_folder/prod_release).
* Global Tags
	* A regex to match a job, and a list of tags to apply to that job, all separated by a comma. 
	  * tags can reference match groups in the regex using the $ symbol 
	  * eg: `(.*?)_job_(*?)_release, owner:$1, release_env:$2, optional:Tag3`

From a job specific configuration page
* Custom tags
	* From a file in the job workspace (not compatible with Pipeline jobs).
	* As text properties directly from the configuration page.

## Installation
_This plugin requires [Jenkins 1.580.1](http://updates.jenkins-ci.org/download/war/1.580.1/jenkins.war) or newer._

This plugin can be installed from the [Update Center](https://wiki.jenkins-ci.org/display/JENKINS/Plugins#Plugins-Howtoinstallplugins) (found at `Manage Jenkins -> Manage Plugins`) in your Jenkins installation. Select the `Available` tab, search for `Datadog` and look for `Datadog Plugin`. Once you find it, check the checkbox next to it, and install via your preference by using one of the two install buttons at the bottom of the screen. Check to see that the plugin has been successfully installed by searching for `Datadog Plugin` on the `Installed` tab. If the plugin has been successfully installed, then continue on to the configuration step, described below.

Note: If you do not see the version of `Datadog Plugin` that you are expecting, make sure you have run `Check Now` from the `Manage Jenkins -> Manage Plugins` screen.

## Configuration
To configure your newly installed Datadog Plugin, simply navigate to the `Manage Jenkins -> Configure System` page on your Jenkins installation. Once there, scroll down to find the `Datadog Plugin` section. Find your API Key from the [API Keys](https://app.datadoghq.com/account/settings#api) page on your Datadog account, and copy/paste it into the `API Key` textbox on the Jenkins configuration screen. You can test that your API Key works by pressing the `Test Key` button, on the Jenkins configuration screen, directly below the API Key textbox. Once your configuration changes are finished, simply save them, and you're good to go!

Alternatively, you have the option of configuring your Datadog plugin using a Groovy script like this one:

```groovy
import jenkins.model.*
import org.datadog.jenkins.plugins.datadog.DatadogGlobalConfiguration

def j = Jenkins.getInstance()
def d = j.getDescriptor("org.datadog.jenkins.plugins.datadog.DatadogGlobalConfiguration")
d.setHostname('https://your-jenkins.com:8080')
d.setApiKey('XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX')
d.setBlacklist('job1,job2')
d.save()
```

Configuring the plugin this way might be useful if you're running your Jenkins Master in a Docker container using the [Official Jenkins Docker Image](https://github.com/jenkinsci/docker) or any derivative that supports plugins.txt and Groovy init scripts.

### Logging
Logging is done by utilizing the java.util.Logger, which follows the [best logging practices for Jenkins](https://wiki.jenkins-ci.org/display/JENKINS/Logging). In order to obtain logs, follow the directions listed [here](https://wiki.jenkins-ci.org/display/JENKINS/Logging). When adding a Logger, all Datadog plugin functions start with `org.datadog.jenkins.plugins.datadog.` and the function name you're after should autopopulate. As of this writing, the only function available was `org.datadog.jenkins.plugins.datadog.listeners.DatadogBuildListener`.

## Release Process
### Overview
Our [DataDog/jenkins-datadog-plugin](https://github.com/DataDog/jenkins-datadog-plugin) repository handles the most up-to-date changes we've made to the Datadog Plugin, as well as issue tickets revolving around that work. Releases are merged to the [Jenkins-CI git repo for our plugin](https://github.com/jenkinsci/datadog-plugin), and represents the source used for plugin releases found in the [Update Center](https://wiki.jenkins-ci.org/display/JENKINS/Plugins#Plugins-Howtoinstallplugins) in your Jenkins installation.

Every commit to our [DataDog/jenkins-datadog-plugin](https://github.com/DataDog/jenkins-datadog-plugin) repository triggers a Jenkins build on our internal Jenkins installation.

A list of our releases is [here](https://github.com/jenkinsci/datadog-plugin/releases).

### How to Release
To release a new plugin version, change the project version in the [pom.xml](pom.xml) from x.x.x-SNAPSHOT to the updated version number you'd like to see. Add an entry for the new release number to [CHANGELOG.md](CHANGELOG.md), and ensure that all the changes are listed accurately. Then run the `jenkins-datadog-plugin-release` job in our Jenkins installation. If the job completes successfully, then the newly updated plugin should be available from the Jenkins [Update Center](https://wiki.jenkins-ci.org/display/JENKINS/Plugins#Plugins-Howtoinstallplugins) within ~4 hours (plus mirror propogation time).

## Issue Tracking
We use Github's built in issue tracking system for all issues tickets relating to this plugin, found [here](https://github.com/DataDog/jenkins-datadog-plugin/issues). However, given how Jenkins Plugins are hosted, there may be issues that are posted to JIRA as well. You can check [here](https://issues.jenkins-ci.org/issues/?jql=project%20%3D%20JENKINS%20AND%20status%20in%20%28Open%2C%20%22In%20Progress%22%2C%20Reopened%29%20AND%20component%20%3D%20datadog-plugin%20ORDER%20BY%20updated%20DESC%2C%20priority%20DESC%2C%20created%20ASC) for those issue postings.

[Here](https://issues.jenkins-ci.org/browse/INFRA-305?jql=status%20in%20%28Open%2C%20%22In%20Progress%22%2C%20Reopened%2C%20Verified%2C%20Untriaged%2C%20%22Fix%20Prepared%22%29%20AND%20text%20~%20%22datadog%22) are unresolved issues on JIRA mentioning Datadog.

## Changes
See the [CHANGELOG.md](CHANGELOG.md)

# How to contribute code

First of all and most importantly, **thank you** for sharing.

If you want to submit code, please fork this repository and submit pull requests against the `master` branch.
For more information, checkout the [contributing guidelines](https://github.com/DataDog/datadog-agent/blob/master/CONTRIBUTING.md) for our agent. We'll attempt to follow these here, as well, where it makes sense.

Check out the [development document](CONTRIBUTING.md) for tips on spinning up a quick development environment locally.

# Manual Testing
In order to keep track of some testing procedures for ensuring proper functionality of the Datadog Plugin on Jenkins, there is a [testing document](CONTRIBUTING.md).

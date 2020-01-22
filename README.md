# IMPORTANT
This repository is now archived.  
Official repository is now located at [https://github.com/jenkinsci/datadog-plugin](https://github.com/jenkinsci/datadog-plugin).  
This repository will not be deleted in order to keep track of historical changes, issues and pull requests.
  - Note that the changelog file references issues from this repository. 
_________
[![Build Status](https://dev.azure.com/datadoghq/jenkins-datadog-plugin/_apis/build/status/DataDog.jenkins-datadog-plugin?branchName=master)](https://dev.azure.com/datadoghq/jenkins-datadog-plugin/_build/latest?definitionId=18&branchName=master)

# Jenkins Datadog Plugin
A Jenkins plugin used to forward metrics, events, and service checks to an account at Datadog, automatically.

There is a [Jenkins CI Plugin page](https://plugins.jenkins.io/datadog) for this plugin, but it refers to our [DataDog/jenkins-datadog-plugin](https://github.com/DataDog/jenkins-datadog-plugin) documentation.

## Features
Currently, the plugin is tracking the following data.

### Events
#### Default Events Type
* Build Started 
  - Triggered on `RunListener#onStarted`
  - Default tags: `job`, `node`, `branch`
  - Associated rate metric: `jenkins.job.started`
* Build Aborted 
  - Triggered on `RunListener#onDeleted`
  - Default tags: `job`, `node`, `branch`
  - Associated rate metric: `jenkins.job.aborted`
* Build Completed 
  - Triggered on `RunListener#onCompleted`
  - Default tags: `job`, `node`, `branch`, `result` (Git Branch, SVN revision or CVS branch)
  - Associated rate metric: `jenkins.job.completed`

#### Source Control Management Events Type   
* SCM Checkout 
  - Triggered on `SCMListener#onCheckout`
  - Default tags: `job`, `node`, `branch`
  - Associated rate metric: `jenkins.scm.checkout`
  
#### Systems Events Type
* Computer Online 
  - Triggered on `ComputerListener#onOnline`
  - Associated rate metric: `jenkins.computer.online`
* Computer Offline 
  - Triggered on `ComputerListener#onOffline`
  - Associated rate metric: `jenkins.computer.online`
* Computer TemporarilyOnline 
  - Triggered on `ComputerListener#onTemporarilyOnline`
  - Associated rate metric: `jenkins.computer.temporarily_online`
* Computer TemporarilyOffline 
  - Triggered on `ComputerListener#onTemporarilyOffline`
  - Associated rate metric: `jenkins.computer.temporarily_offline`
* Computer LaunchFailure
  - Triggered on `ComputerListener#onLaunchFailure`
  - Associated rate metric: `jenkins.computer.launch_failure`
* Item Created
  - Triggered on `ItemListener#onCreated`
  - Associated rate metric: `jenkins.item.created`
* Item Deleted
  - Triggered on `ItemListener#onDeleted`
  - Associated rate metric: `jenkins.item.deleted`
* Item Updated
  - Triggered on `ItemListener#onUpdated`
  - Default tags: 
  - Associated rate metric: `jenkins.item.updated`
* Item Copied
  - Triggered on `ItemListener#onCopied`
  - Associated rate metric: `jenkins.item.copied`
* ItemListener LocationChanged    
  - Triggered on `ItemListener#onLocationChanged`
  - Associated rate metric: `jenkins.item.location_changed`
* Config Changed
  - Triggered on `SaveableListener#onChange`
  - Associated rate metric: `jenkins.config.changed`
  
#### Security Events Type
* User Authenticated
  - Triggered on `SecurityListener#authenticated`
  - Default tags: 
  - Associated rate metric: `jenkins.user.authenticated`
* User failed To Authenticate 
  - Triggered on `SecurityListener#failedToAuthenticate`
  - Associated rate metric: `jenkins.user.access_denied`
* User loggedOut
  - Triggered on `SecurityListener#loggedOut`
  - Associated rate metric: `jenkins.user.logout`

### Metrics

| Metric Name                            | Description                                                    | Default Tags                               |
|----------------------------------------|----------------------------------------------------------------|--------------------------------------------|
| `jenkins.computer.launch_failure`      | Rate of computer launch failures.                              |                                            |
| `jenkins.computer.offline`             | Rate of computer going offline.                                |                                            |
| `jenkins.computer.online`              | Rate of computer going online.                                 |                                            |
| `jenkins.computer.temporarily_offline` | Rate of computer going temporarily offline.                    |                                            |
| `jenkins.computer.temporarily_online`  | Rate of computer going temporarily online.                     |                                            |
| `jenkins.config.changed`               | Rate of configs being changed.                                 |                                            |
| `jenkins.executor.count`               | Executor count.                                                | `node_hostname`, `node_name`, `node_label` |
| `jenkins.executor.free`                | Number of unused executor.                                     | `node_hostname`, `node_name`, `node_label` |
| `jenkins.executor.in_use`              | Number of idle executor.                                       | `node_hostname`, `node_name`, `node_label` |
| `jenkins.item.copied`                  | Rate of items being copied.                                    |                                            |
| `jenkins.item.created`                 | Rate of items being created.                                   |                                            |
| `jenkins.item.deleted`                 | Rate of items being deleted.                                   |                                            |
| `jenkins.item.location_changed`        | Rate of items being moved.                                     |                                            |
| `jenkins.item.updated`                 | Rate of items being updated.                                   |                                            |
| `jenkins.job.aborted`                  | Rate of aborted jobs.                                          | `branch`, `job`, `node`                    |
| `jenkins.job.completed`                | Rate of completed jobs.                                        | `branch`, `job`, `node`, `result`          |
| `jenkins.job.cycletime`                | Build Cycle Time.                                              | `branch`, `job`, `node`, `result`          |
| `jenkins.job.duration`                 | Build duration (in seconds).                                   | `branch`, `job`, `node`, `result`          |
| `jenkins.job.feedbacktime`             | Feedback time from code commit to job failure.                 | `branch`, `job`, `node`, `result`          |
| `jenkins.job.leadtime`                 | Build Lead Time.                                               | `branch`, `job`, `node`, `result`          |
| `jenkins.job.mtbf`                     | MTBF, time between last successful job and current failed job. | `branch`, `job`, `node`, `result`          |
| `jenkins.job.mttr`                     | MTTR: time between last failed job and current successful job. | `branch`, `job`, `node`, `result`          |
| `jenkins.job.started`                  | Rate of started jobs.                                          | `branch`, `job`, `node`                    |
| `jenkins.job.waiting`                  | Time spent waiting for job to run (in milliseconds).           | `branch`, `job`, `node`                    |
| `jenkins.node.count`                   | Total number of node.                                          |                                            |
| `jenkins.node.offline`                 | Offline nodes count.                                           |                                            | 
| `jenkins.node.online`                  | Online nodes count.                                            |                                            |
| `jenkins.plugin.count`                 | Plugins count.                                                 |                                            |
| `jenkins.project.count`                | Project count.                                                 |                                            |
| `jenkins.queue.size`                   | Queue Size.                                                    |                                            |
| `jenkins.queue.buildable`              | Number of Buildable item in Queue.                             |                                            |
| `jenkins.queue.pending`                | Number of Pending item in Queue.                               |                                            |
| `jenkins.queue.stuck`                  | Number of Stuck item in Queue.                                 |                                            |
| `jenkins.queue.blocked`                | Number of Blocked item in Queue.                               |                                            |
| `jenkins.scm.checkout`                 | Rate of SCM checkouts.                                         | `branch`, `job`, `node`                    |
| `jenkins.user.access_denied`           | Rate of users failing to authenticate.                         |                                            |
| `jenkins.user.authenticated`           | Rate of users authenticating.                                  |                                            |
| `jenkins.user.logout`                  | Rate of users logging out.                                     |                                            |


### Service checks
* Build status `jenkins.job.status`
  - Default tags: : `job`, `node`, `branch`, `result` (Git Branch, SVN revision or CVS branch)
    - NOTE: Git Branch available when using the [Git Plugin](https://wiki.jenkins.io/display/JENKINS/Git+Plugin)

## Customization

From the global configuration page, at `Manage Jenkins -> Configure System`.
* Blacklisted Jobs
	* A comma-separated list of regex to match job names that should not be monitored. (eg: susans-job,johns-.*,prod_folder/prod_release).
	* This property can be set using the following environment variable: `DATADOG_JENKINS_PLUGIN_BLACKLIST`.
* Whitelisted Jobs
	* A comma-separated list of regex to match job names that should be monitored. (eg: susans-job,johns-.*,prod_folder/prod_release).
	* This property can be set using the following environment variable: `DATADOG_JENKINS_PLUGIN_WHITELIST`.
* Global Tag File
    * Path to the workspace file containing a comma separated list of tags (not compatible with Pipeline jobs).   	
    * This property can be set using the following environment variable: `DATADOG_JENKINS_PLUGIN_GLOBAL_TAG_FILE`.
* Global Tags
	* A comma-separated list of tags to apply to all metrics, events, service checks.
	* This property can be set using the following environment variable: `DATADOG_JENKINS_PLUGIN_GLOBAL_TAGS`.    
* Global Job Tags
	* A regex to match a job, and a list of tags to apply to that job, all separated by a comma. 
	  * tags can reference match groups in the regex using the $ symbol 
	  * eg: `(.*?)_job_(*?)_release, owner:$1, release_env:$2, optional:Tag3`
	  * This property can be set using the following environment variable: `DATADOG_JENKINS_PLUGIN_GLOBAL_JOB_TAGS`.
* Send Security audit events
    * Enabled by default, it submits `Security Events Type` of events and metrics.
    * This property can be set using the following environment variable: `DATADOG_JENKINS_PLUGIN_EMIT_SECURITY_EVENTS`.
* Send System events
    * Enabled by default, it submits `System Events Type` of events and metrics
    * This property can be set using the following environment variable: `DATADOG_JENKINS_PLUGIN_EMIT_SYSTEM_EVENTS`.

From a job specific configuration page
* Custom tags
	* From a `File` in the job workspace (not compatible with Pipeline jobs). If set, it will override the `Global Job Tags` configuration. 
	* As text `Properties` directly from the configuration page.
* Send Source Control Management events	
    * Enabled by default, it submits `Source Control Management Events Type` of events and metrics.

## Installation
_This plugin requires [Jenkins 1.580.1](http://updates.jenkins-ci.org/download/war/1.580.1/jenkins.war) or newer._

This plugin can be installed from the [Update Center](https://wiki.jenkins-ci.org/display/JENKINS/Plugins#Plugins-Howtoinstallplugins) 
(found at `Manage Jenkins -> Manage Plugins`) in your Jenkins installation. 
Select the `Available` tab, search for `Datadog` and look for `Datadog Plugin`. 
Once you find it, check the checkbox next to it, and install via your preference by using one of the two install buttons at the bottom of the screen. 
Check to see that the plugin has been successfully installed by searching for `Datadog Plugin` on the `Installed` tab. 
If the plugin has been successfully installed, then continue on to the configuration step, described below.

Note: If you do not see the version of `Datadog Plugin` that you are expecting, make sure you have run `Check Now` from the `Manage Jenkins -> Manage Plugins` screen.

## Configuration

### Configure with the plugin user interface

To configure your newly installed Datadog Plugin, navigate to the `Manage Jenkins -> Configure System` page on your Jenkins installation. 
Once there, scroll down to find the `Datadog Plugin` section.

You can use two ways to configure your plugin to submit data to Datadog.
- By using a Datadog API Key.
  - Click the "Use Datadog API URL and Key to report to Datadog" radio button (selected by default)
  - Find your API Key from the [API Keys](https://app.datadoghq.com/account/settings#api) page on your Datadog account, and copy/paste it into the `API Key` textbox on the Jenkins configuration screen.
  - You can test that your API Key works by pressing the `Test Key` button, on the Jenkins configuration screen, directly below the API Key textbox.
- By using a DogStatsD server.
  - Click the "Use a DogStatsD Server to report to Datadog" radio button.
  - Specify both your DogStatD server hostname and port
   
Once your configuration changes are finished, save them, and you're good to go!

### Configure with a Groovy script

Configure your Datadog plugin using a Groovy script like this one

```groovy
import jenkins.model.*
import org.datadog.jenkins.plugins.datadog.DatadogGlobalConfiguration

def j = Jenkins.getInstance()
def d = j.getDescriptor("org.datadog.jenkins.plugins.datadog.DatadogGlobalConfiguration")

// If you want to use Datadog API URL and Key to report to Datadog
d.setReportWith('HTTP')
d.setTargetApiURL('https://your-jenkins.com:8080')
d.setTargetApiKey('XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX')

// If you want to use a DogStatsD Server to report to Datadog
// d.setReportWith('DSD')
// d.setTargetHost('localhost')
// d.setTargetPort(8125)

// Other configs
d.setBlacklist('job1,job2')
// Save config
d.save()
```

Configuring the plugin this way might be useful if you're running your Jenkins Master in a Docker container using the [Official Jenkins Docker Image](https://github.com/jenkinsci/docker) or any derivative that supports plugins.txt and Groovy init scripts.

### Configure with an environment variables

Configure your Datadog plugin using environment variables by specifying the three variables below:. 
- `DATADOG_JENKINS_PLUGIN_REPORT_WITH` which specifies which report mechanism you want to use. When set to `DSD` it will use a DogStatsD Server to report to Datadog. Otherwise set it to the default `HTTP` value.

If you set `DATADOG_JENKINS_PLUGIN_REPORT_WITH` with the `DSD` value, you must specify the following environment variables:
- `DATADOG_JENKINS_PLUGIN_TARGET_HOST` which specifies the DogStatsD Server host to report to. Default value is `localhost`.
- `DATADOG_JENKINS_PLUGIN_TARGET_PORT` which specifies the DogStatsD Server port to report to. Default value is `8125`.

If you set `DATADOG_JENKINS_PLUGIN_REPORT_WITH` with the `HTTP` value or don't specify it, you must specify the following environment variables:
- `DATADOG_JENKINS_PLUGIN_TARGET_API_URL` which specifies the Datadog API Endpoint to report to. Default value is `https://api.datadoghq.com/api/`.
- `DATADOG_JENKINS_PLUGIN_TARGET_API_KEY` which specifies your Datadog API key in order to report to your Datadog account.
  - Get your API Key from the [Datadog API Keys page](https://app.datadoghq.com/account/settings#api).

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

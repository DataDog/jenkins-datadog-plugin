# Jenkins Datadog Plugin

[![Build Status](https://dev.azure.com/datadoghq/jenkins-datadog-plugin/_apis/build/status/DataDog.jenkins-datadog-plugin?branchName=master)](https://dev.azure.com/datadoghq/jenkins-datadog-plugin/_build/latest?definitionId=18&branchName=master)

A Jenkins plugin for automatically forwarding metrics, events, and service checks to a Datadog account.

**Note**: The [Jenkins CI plugin page][1] for this plugin references this documentation.

## Setup

### Installation

_This plugin requires [Jenkins 1.580.1][2] or newer._

This plugin can be installed from the [Update Center][3] (found at `Manage Jenkins -> Manage Plugins`) in your Jenkins installation:

1. Select the `Available` tab, search for `Datadog`, and select the checkbox next to `Datadog Plugin`.
2. Install the plugin by using one of the two install buttons at the bottom of the screen.
3. To verify the plugin is installed, search for `Datadog Plugin` on the `Installed` tab. After the plugin is installed successfully, continue to the configuration section below.

**Note**: If you see an unexpected version of the `Datadog Plugin`, run `Check Now` from the `Manage Jenkins -> Manage Plugins` screen.

### Configuration

You can use two ways to configure your plugin to submit data to Datadog:

* Sending the data directly to Datadog through HTTP.
* Using a DogStatsD server that acts as a forwarder between Jenkins and Datadog.

The configuration can be done from the [plugin user interface](#plugin-user-interface) with a [Groovy script](#groovy-script), or through [environment variables](#environment-variables).

#### Plugin user interface

To configure your Datadog Plugin, navigate to the `Manage Jenkins -> Configure System` page on your Jenkins installation. Once there, scroll down to find the `Datadog Plugin` section:

##### HTTP forwarding {#http-forwarding-plugin}

1. Select the radio button next to **Use Datadog API URL and Key to report to Datadog** (selected by default).
2. Use your [Datadog API key][4] in the `API Key` textbox on the Jenkins configuration screen.
3. Test your Datadog API key by using the `Test Key` button on the Jenkins configuration screen directly below the API key textbox.
4. Save your configuration.

##### DogStatsD forwarding {#dogstatsd-forwarding-plugin}

1. Select the radio button next to **Use a DogStatsD Server to report to Datadog**.
2. Specify your DogStatD server `hostname` and `port`.
3. Save your configuration.

#### Groovy script

Configure your Datadog plugin to forward data through HTTP or DogStatsD using the Groovy scripts below. Configuring the plugin this way might be useful if you're running your Jenkins Master in a Docker container using the [official Jenkins Docker image][5] or any derivative that supports `plugins.txt` and Groovy init scripts.

##### HTTP forwarding {#http-forwarding-groovy-script}

```groovy
import jenkins.model.*
import org.datadog.jenkins.plugins.datadog.DatadogGlobalConfiguration

def j = Jenkins.getInstance()
def d = j.getDescriptor("org.datadog.jenkins.plugins.datadog.DatadogGlobalConfiguration")

// If you want to use Datadog API URL and Key to report to Datadog
d.setReportWith('HTTP')
d.setTargetApiURL('https://api.datadoghq.com/api/')
d.setTargetApiKey('<DATADOG_API_KEY>')

// Customization, see dedicated section below
d.setBlacklist('job1,job2')

// Save config
d.save()
```

##### DogStatsD forwarding {#dogstatsd-forwarding-groovy-script}

```groovy
import jenkins.model.*
import org.datadog.jenkins.plugins.datadog.DatadogGlobalConfiguration

def j = Jenkins.getInstance()
def d = j.getDescriptor("org.datadog.jenkins.plugins.datadog.DatadogGlobalConfiguration")

d.setReportWith('DSD')
d.setTargetHost('localhost')
d.setTargetPort(8125)

// Customization, see dedicated section below
d.setBlacklist('job1,job2')

// Save config
d.save()
```

#### Environment variables

Configure your Datadog plugin using environment variables with the `DATADOG_JENKINS_PLUGIN_REPORT_WITH` variable, which specifies the report mechanism to use.

##### HTTP forwarding {#http-forwarding-env}

1. Set the `DATADOG_JENKINS_PLUGIN_REPORT_WITH` variable to `HTTP`.
2. Set the `DATADOG_JENKINS_PLUGIN_TARGET_API_URL` variable, which specifies the Datadog API endpoint (defaults `https://api.datadoghq.com/api/`).
3. Set the `DATADOG_JENKINS_PLUGIN_TARGET_API_KEY` variable, which specifies your [Datadog API key][4].

##### DogStatsD forwarding {#dogstatsd-forwarding-env}

1. Set the `DATADOG_JENKINS_PLUGIN_REPORT_WITH` variable to `DSD`.
2. Set the `DATADOG_JENKINS_PLUGIN_TARGET_HOST` variable, which specifies the DogStatsD server host (defaults to `localhost`).
3. Set the `DATADOG_JENKINS_PLUGIN_TARGET_PORT` variable, which specifies the DogStatsD server port (defaults to `8125`).

#### Logging

Logging is done by utilizing the `java.util.Logger`, which follows the [best logging practices for Jenkins][6]. To obtain logs, follow the directions in the [Jenkins logging documentation][6]. When adding a logger, all Datadog plugin functions start with `org.datadog.jenkins.plugins.datadog.` and the function name you are after should autopopulate. As of this writing, the only function available was `org.datadog.jenkins.plugins.datadog.listeners.DatadogBuildListener`.

## Customization

### Global customization

From the global configuration page, at `Manage Jenkins -> Configure System` you can customize your configuration with:

| Customization              | Description                                                                                                                                                                                                                                 | Environment variable                          |
|----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------|
| Blacklisted jobs           | A comma-separated list of regex used to exclude job names from monitoring, for example: `susans-job,johns-.*,prod_folder/prod_release`.                                                                                                      | `DATADOG_JENKINS_PLUGIN_BLACKLIST`            |
| Whitelisted jobs           | A comma-separated list of regex used to include job names for monitoring, for example: `susans-job,johns-.*,prod_folder/prod_release`.                                                                                                          | `DATADOG_JENKINS_PLUGIN_WHITELIST`            |
| Global tag file            | The path to a workspace file containing a comma separated list of tags (not compatible with pipeline jobs).                                                                                                                                   | `DATADOG_JENKINS_PLUGIN_GLOBAL_TAG_FILE`      |
| Global tags                | A comma-separated list of tags to apply to all metrics, events, and service checks.                                                                                                                                                         | `DATADOG_JENKINS_PLUGIN_GLOBAL_TAGS`          |
| Global job tags            | A comma separated list of regex to match a job and a list of tags to apply to that job. **Note**: Tags can reference match groups in the regex using the `$` symbol, for example: `(.*?)_job_(*?)_release, owner:$1, release_env:$2, optional:Tag3` | `DATADOG_JENKINS_PLUGIN_GLOBAL_JOB_TAGS`      |
| Send security audit events | Submits the `Security Events Type` of events and metrics (enabled by default).                                                                                                                                                                | `DATADOG_JENKINS_PLUGIN_EMIT_SECURITY_EVENTS` |
| Send system events         | Submits the `System Events Type` of events and metrics (enabled by default).                                                                                                                                                                  | `DATADOG_JENKINS_PLUGIN_EMIT_SYSTEM_EVENTS`   |

### Job customization

From a job specific configuration page:

| Customization                         | Description                                                                                                                                                                                           |
|---------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Custom tags                           | Set from a `File` in the job workspace (not compatible with pipeline jobs) or as text `Properties` directly from the configuration page. If set, this overrides the `Global Job Tags` configuration. |
| Send source control management events | Submits the `Source Control Management Events Type` of events and metrics (enabled by default).                                                                                                         |

## Data collected

This plugin is collecting the following [events](#events), [metrics](#metrics), and [service checks](#service-checks):

### Events

#### Default events type

| Event name      | Triggered on              | Default tags                                                               | Associated RATE metric  |
|-----------------|---------------------------|----------------------------------------------------------------------------|-------------------------|
| Build started   | `RunListener#onStarted`   | `job`, `node`, `branch`                                                    | `jenkins.job.started`   |
| Build aborted   | `RunListener#onDeleted`   | `job`, `node`, `branch`                                                    | `jenkins.job.aborted`   |
| Build completed | `RunListener#onCompleted` | `job`, `node`, `branch`, `result` (Git branch, SVN revision, or CVS branch) | `jenkins.job.completed` |

#### Source control management events type

| Event name   | Triggered on             | Default tags            | Associated RATE metric |
|--------------|--------------------------|-------------------------|------------------------|
| SCM checkout | `SCMListener#onCheckout` | `job`, `node`, `branch` | `jenkins.scm.checkout` |

#### Systems events type

| Event name                   | Triggered on                            | Associated RATE metric                 |
|------------------------------|-----------------------------------------|----------------------------------------|
| Computer Online              | `ComputerListener#onOnline`             | `jenkins.computer.online`              |
| Computer Offline             | `ComputerListener#onOffline`            | `jenkins.computer.online`              |
| Computer TemporarilyOnline   | `ComputerListener#onTemporarilyOnline`  | `jenkins.computer.temporarily_online`  |
| Computer TemporarilyOffline  | `ComputerListener#onTemporarilyOffline` | `jenkins.computer.temporarily_offline` |
| Computer LaunchFailure       | `ComputerListener#onLaunchFailure`      | `jenkins.computer.launch_failure`      |
| Item Created                 | `ItemListener#onCreated`                | `jenkins.item.created`                 |
| Item Deleted                 | `ItemListener#onDeleted`                | `jenkins.item.deleted`                 |
| Item Updated                 | `ItemListener#onUpdated`                | `jenkins.item.updated`                 |
| Item Copied                  | `ItemListener#onCopied`                 | `jenkins.item.copied`                  |
| ItemListener LocationChanged | `ItemListener#onLocationChanged`        | `jenkins.item.location_changed`        |
| Config Changed               | `SaveableListener#onChange`             | `jenkins.config.changed`               |

#### Security events type

| Event name                  | Triggered on                            | Associated RATE metric       |
|-----------------------------|-----------------------------------------|------------------------------|
| User Authenticated          | `SecurityListener#authenticated`        | `jenkins.user.authenticated` |
| User failed To Authenticate | `SecurityListener#failedToAuthenticate` | `jenkins.user.access_denied` |
| User loggedOut              | `SecurityListener#loggedOut`            | `jenkins.user.logout`        |

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

Build status `jenkins.job.status` with the default tags: : `job`, `node`, `branch`, `result` (Git branch, SVN revision, or CVS branch)

**Note**: Git `branch` tag is available when using the [Git Plugin][7].

## Release Process

### Overview

The [DataDog/jenkins-datadog-plugin][8] repository handles the most up-to-date changes made to the Datadog Plugin, as well as issue tickets revolving around that work. Releases are merged to the [Jenkins-CI git repo for our plugin][9], and represents the source used for plugin releases found in the [Update Center][3] in your Jenkins installation.

Every commit to the [DataDog/jenkins-datadog-plugin][8] repository triggers a Jenkins build on our internal Jenkins installation.

A list of releases is available at [jenkinsci/datadog-plugin/releases][10].

### How to release

To release a new plugin version, change the project version in the [pom.xml][11] from `x.x.x-SNAPSHOT` to the updated version number you would like to see. Add an entry for the new release number to the [CHANGELOG.md][12] file, and ensure that all the changes are listed accurately. Then run the `jenkins-datadog-plugin-release` job in the Jenkins installation. If the job completes successfully, then the newly updated plugin should be available from the Jenkins [Update Center][3] within ~4 hours (plus mirror propogation time).

## Issue Tracking

Github's built-in issue tracking system is used to track all issues relating to this plugin: [DataDog/jenkins-datadog-plugin/issues][13]. However, given how Jenkins plugins are hosted, there may be issues that are posted to JIRA as well. You can check [this jenkins issue][14] for those issue postings.

**Note**: [Unresolved issues on JIRA mentioning Datadog.][15].

## Changes

See the [CHANGELOG.md][12].

## How to contribute code

First of all and most importantly, **thank you** for sharing.

If you want to submit code, fork this repository and submit pull requests against the `master` branch. For more information, checkout the [contributing guidelines][16] for the Datadog Agent.

Check out the [development document][17] for tips on spinning up a quick development environment locally.

## Manual testing

To keep track of testing procedures for ensuring proper functionality of the Datadog Plugin on Jenkins, there is a [testing document][17].

[1]: https://plugins.jenkins.io/datadog
[2]: http://updates.jenkins-ci.org/download/war/1.580.1/jenkins.war
[3]: https://wiki.jenkins-ci.org/display/JENKINS/Plugins#Plugins-Howtoinstallplugins
[4]: https://app.datadoghq.com/account/settings#api
[5]: https://github.com/jenkinsci/docker
[6]: https://wiki.jenkins-ci.org/display/JENKINS/Logging
[7]: https://wiki.jenkins.io/display/JENKINS/Git+Plugin
[8]: https://github.com/DataDog/jenkins-datadog-plugin
[9]: https://github.com/jenkinsci/datadog-plugin
[10]: https://github.com/jenkinsci/datadog-plugin/releases
[11]: pom.xml
[12]: CHANGELOG.md
[13]: https://github.com/DataDog/jenkins-datadog-plugin/issues
[14]: https://issues.jenkins-ci.org/issues/?jql=project%20%3D%20JENKINS%20AND%20status%20in%20%28Open%2C%20%22In%20Progress%22%2C%20Reopened%29%20AND%20component%20%3D%20datadog-plugin%20ORDER%20BY%20updated%20DESC%2C%20priority%20DESC%2C%20created%20ASC
[15]: https://issues.jenkins-ci.org/browse/INFRA-305?jql=status%20in%20%28Open%2C%20%22In%20Progress%22%2C%20Reopened%2C%20Verified%2C%20Untriaged%2C%20%22Fix%20Prepared%22%29%20AND%20text%20~%20%22datadog%22
[16]: https://github.com/DataDog/datadog-agent/blob/master/CONTRIBUTING.md
[17]: CONTRIBUTING.md

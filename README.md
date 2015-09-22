# Jenkins Datadog Plugin
A Jenkins plugin used to forward metrics, events, and service checks to an account at Datadog, automatically.

There is a [Jenkins-CI Wiki page](https://wiki.jenkins-ci.org/display/JENKINS/Datadog+Plugin) for this plugin, but it refers to our [DataDog/jenkins-datadog-plugin](https://github.com/DataDog/jenkins-datadog-plugin) for documentation.

## Features
Currently, the plugin is tracking the following data.

List of events:
* Started build
* Finished build

List of metrics:
* Build duration (jenkins.job.duration)

List of service checks:
* Build status (jenkins.job.status)

All events, metrics, and service checks include the following tags, if they are available:
* job
* node
* result
* branch

## Installation
_This plugin requires [Jenkins 1.580.1](http://updates.jenkins-ci.org/download/war/1.580.1/jenkins.war) or newer._

This plugin can be installed from the [Update Center](https://wiki.jenkins-ci.org/display/JENKINS/Plugins#Plugins-Howtoinstallplugins) (found at `Manage Jenkins -> Manage Plugins`) in your Jenkins installation. Select the `Available` tab, search for `Datadog` and look for `Datadog Build Reporter`. Once you find it, check the checkbox next to it, and install via your preference by using one of the two install buttons at the bottom of the screen. Check to see that the plugin has been successfully installed by searching for `Datadog Build Reporter` on the `Installed` tab. If the plugin has been successfully installed, then continue on to the configuration step, described below.

Note: If you do not see the version of `Datadog Build Reporter` that you are expecting, make sure you have run `Check Now` from the `Manage Jenkins -> Manage Plugins` screen.

## Configuration
To configure your newly installed Datadog Build Reporter plugin, simply navigate to the `Manage Jenkins -> Configure System` page on your Jenkins installation. Once there, scroll down to find the `Datadog Build Reporter` section. Find your API Key from the [API Keys](https://app.datadoghq.com/account/settings#api) page on your Datadog account, and copy/paste it into the `API Key` textbox on the Jenkins configuration screen. You can test that your API Key works by pressing the `Test Key` button, on the Jenkins configuration screen, directly below the API Key textbox. Once your configuration changes are finished, simply save them, and you're good to go!

## Release Process
### Overview
Our [DataDog/jenkins-datadog-plugin](https://github.com/DataDog/jenkins-datadog-plugin) repository handles the most up-to-date changes we've made to the Datadog Build Reporter plugin, as well as issue tickets revolving around that work. Releases are merged to the [Jenkins-CI git repo for our plugin](https://github.com/jenkinsci/datadog-plugin), and represents the source used for plugin releases found in the [Update Center](https://wiki.jenkins-ci.org/display/JENKINS/Plugins#Plugins-Howtoinstallplugins) in your Jenkins installation.

Every commit to our [DataDog/jenkins-datadog-plugin](https://github.com/DataDog/jenkins-datadog-plugin) repository triggers a Jenkins build on our internal Jenkins installation.

A list of our releases is [here](https://github.com/jenkinsci/datadog-plugin/releases).

### How to Release
To release a new plugin version, change the project version in the [pom.xml](pom.xml) from x.x.x-SNAPSHOT to the updated version number you'd like to see. Add an entry for the new release number to [CHANGELOG.md](CHANGELOG.md), and ensure that all the changes are listed accurately. Then run the `jenkins-datadog-plugin-release` job in our Jenkins installation. If the job completes successfully, then the newly updated plugin should be available from the Jenkins [Update Center](https://wiki.jenkins-ci.org/display/JENKINS/Plugins#Plugins-Howtoinstallplugins) within ~4 hours (plus mirror propogation time).

## Issue Tracking
We use Github's built in issue tracking system for all issues tickets relating to this plugin, found [here](https://github.com/DataDog/jenkins-datadog-plugin/issues). However, given how Jenkins Plugins are hosted, there may be issues that are posted to JIRA as well. You can check [here](https://issues.jenkins-ci.org/issues/?jql=project%20%3D%20JENKINS%20AND%20status%20in%20%28Open%2C%20%22In%20Progress%22%2C%20Reopened%29%20AND%20component%20%3D%20datadog-plugin%20ORDER%20BY%20updated%20DESC%2C%20priority%20DESC%2C%20created%20ASC) for those issue postings.

[Here](https://issues.jenkins-ci.org/browse/INFRA-305?jql=status%20in%20%28Open%2C%20%22In%20Progress%22%2C%20Reopened%2C%20Verified%2C%20Untriaged%2C%20%22Fix%20Prepared%22%29%20AND%20text%20~%20%22datadog%22) are unresolved issues on JIRA mentioning Datadog.

## Changes
See the [CHANGELOG.md](CHANGELOG.md)

Changes
=======

# 0.5.2 / 06-23-2016
### Details
https://github.com/jenkinsci/datadog-plugin/compare/datadog-0.5.1...datadog-0.5.2

### Changes
* [BUGFIX] Catch and react to null property in DatadogUtilities.parseTagList(). See [84ec03](https://github.com/DataDog/jenkins-datadog-plugin/commit/84ec0385459928d6f408b7e2c0fe215555550da1)

# 0.5.1 / 06-01-2016
### Details
https://github.com/jenkinsci/datadog-plugin/compare/datadog-0.5.0...datadog-0.5.1

### Changes
* [BUGFIX] Fixed an unhandled NPE caused when DataDog Job Properties were not selected. See [#44](https://github.com/DataDog/jenkins-datadog-plugin/pull/44) (Thanks @MadsNielsen)

# 0.5.0 / 05-24-2016
### Details
https://github.com/jenkinsci/datadog-plugin/compare/datadog-0.4.1...datadog-0.5.0

### Changes
* [IMPROVEMENT] Adding tags by job, via job configuration screen or via workspace text file. See [#38](https://github.com/DataDog/jenkins-datadog-plugin/pull/38) (Thanks @MadsNielsen)
* [IMPROVEMENT] Count metric for completed jobs. See [#39](https://github.com/DataDog/jenkins-datadog-plugin/pull/39) (Thanks @MadsNielsen)

# 0.4.1 / 12-08-2015
### Details
https://github.com/jenkinsci/datadog-plugin/compare/datadog-0.4.0...datadog-0.4.1

### Changes
* [BUGFIX] Fixed issue where apiKey was being returned to the configuration form as hash, causing a test of the key to fail. See [ee95325](https://github.com/DataDog/jenkins-datadog-plugin/commit/ee9532532df99ab998e5f7eb171636905aec6f8c)
* [BUGFIX] Removed a false error log, which was reporting successful POSTs as an error. See [094fbe8](https://github.com/DataDog/jenkins-datadog-plugin/commit/094fbe80cc00378d03d2e357e8e9cfc6f04e86ad)
* [BUGFIX] Round job duration text to the nearest 2 decimals on event messages. See [7bdef98](https://github.com/DataDog/jenkins-datadog-plugin/commit/7bdef98260fc2b42b8c041f39cade6ae3fdb37f8)
* [IMPROVEMENT] Reporting all events as Jenkins source type, enabling proper event display. See [f00b261](https://github.com/DataDog/jenkins-datadog-plugin/commit/f00b26165f040e9bd1996bb1f4fb63ff05c1156f)

# 0.4.0 / 12-04-2015
### Details
https://github.com/jenkinsci/datadog-plugin/compare/datadog-0.3.0...datadog-0.4.0

### Changes
* [IMPROVEMENT] Add support for using a proxy server, utilizing Jenkins proxy settings. See [#30](https://github.com/DataDog/jenkins-datadog-plugin/pull/30) (Thanks @seattletechie)
* [IMPROVEMENT] Replace PrintStream with java.util.Logger, to produce log verbosity control, allowing use of log groups and levels in Jenkins. See [#29](https://github.com/DataDog/jenkins-datadog-plugin/pull/29) (Thanks @dmabamboo)
* [OTHER] Cleaned up blacklist code. See [#28](https://github.com/DataDog/jenkins-datadog-plugin/pull/28) (Thanks @dmabamboo)

# 0.3.0 / 10-19-2015
### Details
https://github.com/jenkinsci/datadog-plugin/compare/datadog-0.2.1...datadog-0.3.0

### Changes
* [IMPROVEMENT] Added the ability to include optional preset tags. See [ea17e44](https://github.com/DataDog/jenkins-datadog-plugin/commit/ea17e44496e5d112196f67c26869969ec15211d4)
* [IMPROVEMENT] Added the ability to blacklist jobs from being reported to DataDog. See [9fde32a](https://github.com/DataDog/jenkins-datadog-plugin/commit/9fde32a699aceaf73de03622147cf39422112197)

# 0.2.1 / 09-25-2015
### Details
https://github.com/jenkinsci/datadog-plugin/compare/datadog-build-reporter-0.2.0...datadog-0.2.1

### Changes
* [BUGFIX] Changed the plugin id from `datadog-build-reporter` to just `datadog`. See [#18](https://github.com/DataDog/jenkins-datadog-plugin/pull/18)

# 0.2.0 / 09-22-2015
### Details
https://github.com/jenkinsci/datadog-plugin/compare/datadog-build-reporter-0.1.3...datadog-build-reporter-0.2.0

### Changes
* [BUGFIX] Improved method of determining the Jenkins hostname. See [#15](https://github.com/DataDog/jenkins-datadog-plugin/pull/15)
* [IMPROVEMENT] Add node tag to events, metrics, and service checks. See [#17](https://github.com/DataDog/jenkins-datadog-plugin/pull/17)
* [OTHER] Remove build_number tag from metrics and service checks. See [#17](https://github.com/DataDog/jenkins-datadog-plugin/pull/17)

# 0.1.3 / 09-04-2015
### Details
https://github.com/jenkinsci/datadog-plugin/compare/datadog-build-reporter-0.1.2...datadog-build-reporter-0.1.3

### Changes
* [BUGFIX] Added a null safe getter function to prevent exceptions when attempting to call `.toString()` on a `null` object. See [#9](https://github.com/DataDog/jenkins-datadog-plugin/pull/9)
* [IMPROVEMENT] Events: Allow for event rollups on Datadog events page.
* [OTHER] Modified build page link to point to the main build page, rather than to the console output.
* [OTHER] Removed build_number tags from events.

# 0.1.2 / 09-01-2015
### Details
Testing automatic release with new Jenkins job.

https://github.com/jenkinsci/datadog-plugin/compare/datadog-build-reporter-0.1.1...datadog-build-reporter-0.1.2

### Changes
* [IMPROVEMENT] Added CHANGELOG.md
* [IMPROVEMENT] Added README.md

# 0.1.1 / 08-28-2015
### Details
Worked out kinks in the release process.

https://github.com/jenkinsci/datadog-plugin/compare/datadog-build-reporter-0.1.0...datadog-build-reporter-0.1.1

### Changes
* [BUGFIX] Javadoc: Fixed javadoc errors in class DatadogBuildListener.
* [BUGFIX] Javadoc: Fixed javadoc errors in method post.

# 0.1.0 / 08-28-2015
### Details
Initial Release

### Changes
* [FEATURE] Events: Started build
* [FEATURE] Events: Finished build
* [FEATURE] Metrics: Build duration (jenkins.job.duration)
* [FEATURE] Service Checks: Build status (jenkins.job.status)

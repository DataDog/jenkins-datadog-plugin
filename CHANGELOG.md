Changes
=======

# 0.2.0 / 09-22-2015
### Details
https://github.com/jenkinsci/datadog-plugin/compare/datadog-build-reporter-0.1.3...datadog-build-reporter-0.2.0

### Changes
* [BUGFIX] Improved method of determining the Jenkins hostname. See [#15][]
* [IMPROVEMENT] Add node tag to events, metrics, and service checks. See [#17][]
* [OTHER] Remove build_number tag from metrics and service checks. See [#17][]

# 0.1.3 / 09-04-2015
### Details
https://github.com/jenkinsci/datadog-plugin/compare/datadog-build-reporter-0.1.2...datadog-build-reporter-0.1.3

### Changes
* [BUGFIX] Added a null safe getter function to prevent exceptions when attempting to call `.toString()` on a `null` object. See [#9][]
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

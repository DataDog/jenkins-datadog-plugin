#
# Datadog Build Reporter Plugin
# Jenkins Dockerfile
#
# Used by Jenkins to test packaging the plugin via maven
#

FROM quay.io/datadog/jenkins-slave

MAINTAINER John Zeller <johnlzeller@gmail.com>

# Setup maven and jdk
RUN apt-get update \
 && apt-get install -y maven default-jdk git

## Developing on this Jenkins Plugin

This page outlines some example steps to spin up a development environment for the *jenkins-datadog-plugin* repository. The requirements are:

* [Java 1.8](https://www.java.com/en/download/)
* [Docker](https://docs.docker.com/get-started/) & [docker-compose](https://docs.docker.com/compose/install/)
* [A clone/fork of this repository](https://help.github.com/en/articles/fork-a-repo)


To get started, save the following `docker-compose.yaml` file in your working directory locally:

```
version: "3.7"
services:
  jenkins:
    image: jenkins/jenkins:lts
    ports:
      - 8080:8080
    volumes:
      - $JENKINS_PLUGIN/target/:/var/jenkins_home/plugins
```

Set the `JENKINS_PLUGIN` environment variable to point to the folder of the clone/fork of this repository.

Then run `docker-compose -f <DOCKER_COMPOSE_FILE_PATH> up -d`.


This spins up the Jenkins docker image and auto mount the target folder of this repository (the location where the binary is built)

To see updates, after re building the provider with `mvn clean package` on your local machine, run `docker-compose down` and spin this up again.

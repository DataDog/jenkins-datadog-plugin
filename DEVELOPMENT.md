## Developing on this Jenkins Plugin

This page will outline some example steps that can be taken to spin up a development enviornment for this repository. The requirements are:

* Java 1.8
* Docker & docker-compose
* A clone/fork of this repository


To get started, save the following docker-compose file somewhere locally:

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

Set the JENKINS_PLUGIN environment variable to point to the folder of the clone or fork of this repository.

From here you can run `docker-compose up -f `<path_to_docker_compose_file>`.

This will spin up the Jenkins docker image and auto mount the target folder of this repository (the location where the binary is built)

To see updates, after re building the provider with `mvn clean package` on your local machine, simply run `docker-compose down` and spin this up again.

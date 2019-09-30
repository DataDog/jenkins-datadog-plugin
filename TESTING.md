# Testing

## Setup

To spin up a development environment for the *jenkins-datadog-plugin* repository. The requirements are:

* [Java 1.8](https://www.java.com/en/download/)
* [Docker](https://docs.docker.com/get-started/) & [docker-compose](https://docs.docker.com/compose/install/)
* [A clone/fork of this repository](https://help.github.com/en/articles/fork-a-repo)


1. To get started, save the following `docker-compose.yaml` file in your working directory locally:

    ```
    version: "3.7"
    services:
      jenkins:
        image: jenkins/jenkins:lts
        ports:
          - 8080:8080
        volumes:
          - $JENKINS_PLUGIN/target/:/var/jenkins_home/plugins
      
      dogstatsd:    
        image: datadog/dogstatsd:latest 
        environment:
          - DD_API_KEY=<API_KEY>
          - DD_LOG_LEVEL=debug
        ports:
          - 8125:8125/udp  
               
    ```

2. Set the `JENKINS_PLUGIN` environment variable to point to the folder of the clone/fork of this repository.
3. Run `docker-compose -f <DOCKER_COMPOSE_FILE_PATH> up`.
4. Check your terminal and look for the admin password:
    ```
    jenkins_1    | *************************************************************
    jenkins_1    | *************************************************************
    jenkins_1    | *************************************************************
    jenkins_1    |
    jenkins_1    | Jenkins initial setup is required. An admin user has been created and a password generated.
    jenkins_1    | Please use the following password to proceed to installation:
    jenkins_1    |
    jenkins_1    | 70dfe3d2b23841cc96c101c0521716f0
    jenkins_1    |
    jenkins_1    | This may also be found at: /var/jenkins_home/secrets/initialAdminPassword
    jenkins_1    |
    jenkins_1    | *************************************************************
    jenkins_1    | *************************************************************
    jenkins_1    | *************************************************************
    ``` 

5. Access your Jenkins instance http://localhost:8080
6. Enter the administrator password in the Getting Started form.
7. On the next page, click on the "Select plugins to be installed" unless you want to installing suggested plugins. 
8. Selected desired plugins depending on your needs. You can always add plugins later.
  - You can skip user creation and stick to the admin user.
9. Create a user so that you don't have to use the admin credential again (optional)
10. Continue until the end of the setup process and log back in.
11. Go to http://localhost:8080/configure to configure the "Datadog Plugin", set your `API Key`.
  - Click on the "Test Key" to make sure your key is valid
  - You can set your machine `hostname`
  - `.*, owner:$1, release_env:$2, optional:Tag3`
  - You can add `node` optional tag by clicking on the node checkbox
  - Click on `Advanced...` and set `DogStatsD Host` to `datadog:8125`. This is in order to submit metrics to the dogStatsD Server.

## Create your first job

1. On jenkins Home page, click on "Create a new Job" 
2. Give it a name and select "freestyle project" (unless you want to do something else).
3. Then add a build step (execute Shell):
    ```
    #!/bin/sh
    
    echo "Executing my job script"
    sleep 5s
    ```

## Create Logger
1. Go to http://localhost:8080/log/
2. Give a name to your logger - For example `datadog`
3. Add entries for all `org.datadog.jenkins.plugins.datadog.*` packages with log Level `ALL`.
4. If you now run a job and go back to http://localhost:8080/log/datadog/, you should see your logs

This spins up the Jenkins docker image and auto mount the target folder of this repository (the location where the binary is built)

To see updates, after re building the provider with `mvn clean package` on your local machine, run `docker-compose down` and spin this up again.

# Contributing

We love pull requests. Here's a quick guide.

Fork, then clone the repo:

    git clone git@github.com:DataDog/jenkins-datadog-plugin.git

Make sure the tests pass:

    mvn test

Make your change. Add tests for your change. Make the tests pass again.
It is strongly recommended to perform manual testing as well, see section below.


Push to your fork and [submit a pull request][pr].

[pr]: https://github.com/your-username/jenkins-datadog-plugin/compare/DataDog:master...master

At this point you're waiting on us. We may suggest some changes or
improvements or alternatives.

## Manual Testing

### Setup

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
               
    ```

2. Set the `JENKINS_PLUGIN` environment variable to point to the directory where this repository is cloned/forked.
3. Run `docker-compose -f <DOCKER_COMPOSE_FILE_PATH> up`.
    - NOTE: This spins up the Jenkins docker image and auto mount the target folder of this repository (the location where the binary is built)
    - NOTE: To see code updates, after re building the provider with `mvn clean package` on your local machine, run `docker-compose down` and spin this up again.
4. Check your terminal and look for the admin password:
    ```
    jenkins_1    | *************************************************************
    jenkins_1    | *************************************************************
    jenkins_1    | *************************************************************
    jenkins_1    |
    jenkins_1    | Jenkins initial setup is required. An admin user has been created and a password generated.
    jenkins_1    | Please use the following password to proceed to installation:
    jenkins_1    |
    jenkins_1    | <JENKINS_ADMIN_PASSWORD>
    jenkins_1    |
    jenkins_1    | This may also be found at: /var/jenkins_home/secrets/initialAdminPassword
    jenkins_1    |
    jenkins_1    | *************************************************************
    jenkins_1    | *************************************************************
    jenkins_1    | *************************************************************
    ``` 

5. Access your Jenkins instance http://localhost:8080
6. Enter the administrator password in the Getting Started form.
7. On the next page, click on the "Select plugins to be installed" unless you want to install all suggested plugins. 
8. Select desired plugins depending on your needs. You can always add plugins later.
9. Create a user so that you don't have to use the admin credential again (optional).
10. Continue until the end of the setup process and log back in.
11. Go to http://localhost:8080/configure to configure the "Datadog Plugin", set your `API Key`.
  - Click on the "Test Key" to make sure your key is valid.
  - You can set your machine `hostname`.
  - You can set Global Tag. For example `.*, owner:$1, release_env:$2, optional:Tag3`.
  
### Create your first job

1. On jenkins Home page, click on "Create a new Job" 
2. Give it a name and select "freestyle project".
3. Then add a build step (execute Shell):
    ```
    #!/bin/sh
    
    echo "Executing my job script"
    sleep 5s
    ```

### Create Logger
1. Go to http://localhost:8080/log/
2. Give a name to your logger - For example `datadog`
3. Add entries for all `org.datadog.jenkins.plugins.datadog.*` packages with log Level `ALL`.
4. If you now run a job and go back to http://localhost:8080/log/datadog/, you should see your logs

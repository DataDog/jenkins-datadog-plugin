# Testing
This document serves as a place to document manual testing setups for ensuring proper functionality of the Jenkins plugin.

## Testing Proxy
In order to test the proxy server functionality, from Jenkins, we need a development setup.

Utilized the following docker containers:
* https://github.com/sameersbn/docker-squid
* https://github.com/jenkinsci/docker

### Make squid proxy server
Pull down the `sameersbn/squid` docker image, and run while exposing port 3128.

    docker pull sameersbn/squid:3.3.8-4
    docker build -t sameersbn/squid github.com/sameersbn/docker-squid
    docker run --name squid -d --restart=always \
      --publish 3128:3128 \
      --volume /srv/docker/squid/cache:/var/spool/squid3 \
      sameersbn/squid:3.3.8-4

### Make jenkins server
Run a Jenkins docker image, exposing ports 8080 and 50000:

    docker run -d --name web -p 8080:8080 -p 50000:50000 jenkins

From here, you'll connect to Jenkins UI from your browser, through `localhost:8080`. Once you are connected, follow these steps:

1. Install the Datadog Plugin, either via the Update Center, or via .hpi
2. Configure with an API key
3. Create a test build

Now, we need to connect to the squid docker container to do some introspection, so we can prove that the proxy is routing this data.

    docker exec -it squid bash

Once you are conencted to the squid box, run a tcp dump to follow port 3128.

    apt-get update
    apt-get install -y tcpdump
    sudo tcpdump -i eth0 -vvvvtttAXns 1500 'port 3128'

Now that this window is following all the traffic coming through port 3128, let's go back to the Jenkins UI.

1. Run the test build that you had setup previously.
2. Look on the output from the tcpdump. You should NOT see any output.
3. Now go to `Manage Jenkins` > `Manage Plugins`, and then select the `Advanced` tab.
4. From here you will see the 'HTTP Proxy Configuration' section. Enter the following information:
    * Server: 172.17.42.1
    * Port: 3128
5. Optionally, you can click `Advanced`, and then test `http://www.google.com` and you should see the output in the tcpdump terminal window.
6. Finally, run the test build again.
7. You should now see output from the tcpdump terminal window!

Repeat this by enabling/disabling the proxy configuration in Jenkins and repeating the test to prove to yourself that it was not a fluke.
**Note: I've noticed that Jenkins seems to send a disconnection series of packets to the proxy server with the first connection attempt (when you run your test build), after you've disabled the proxy configration. Run the test job a second time, and you'll again not see any output.**

## Check Style
In order to check that the Java style meets the recommendations of Sun and Google, as closely as possible, here is a way to test it.

1. Download the most recent [checkstyle jar](http://sourceforge.net/projects/checkstyle/files/checkstyle/) from SourceForge.
2. Grab the style checks from [Sun](https://raw.githubusercontent.com/checkstyle/checkstyle/master/src/main/resources/sun_checks.xml) and [Google](https://raw.githubusercontent.com/checkstyle/checkstyle/master/src/main/resources/google_checks.xml).
 * Note: We following 100 character line length, so remove the `<module name="LineLength"/>` line from sun_check.xml.
3. Run each check, one at a time:

    ```bash
    java -jar checkstyle-6.13-all.jar -c /sun_checks.xml MyClass.java
    java -jar checkstyle-6.13-all.jar -c /google_checks.xml MyClass.java
    ```

More detailed instructions on using checkstyle can be found [here](http://checkstyle.sourceforge.net/cmdline.html).

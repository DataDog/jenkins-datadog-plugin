package org.datadog.jenkins.plugins.datadog.publishers;

import hudson.Extension;
import hudson.model.*;
import org.datadog.jenkins.plugins.datadog.DatadogClient;
import org.datadog.jenkins.plugins.datadog.clients.ClientFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Extension
public class DatadogCountersPublisher extends AsyncPeriodicWork {

    private static final Logger logger = Logger.getLogger(DatadogCountersPublisher.class.getName());

    public DatadogCountersPublisher() {
        super("Datadog Counters Publisher");
    }

    @Override
    public long getRecurrencePeriod() {
        return TimeUnit.SECONDS.toMillis(10);
    }

    @Override
    protected void execute(TaskListener taskListener) throws IOException, InterruptedException {
        try {
            logger.fine("Execute called: Publishing counters");

            // Get Datadog Client Instance
            DatadogClient client = ClientFactory.getClient();
            client.flushCounters();
        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }
    }
}

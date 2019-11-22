package org.datadog.jenkins.plugins.datadog.publishers;

import hudson.Extension;
import hudson.model.*;
import org.datadog.jenkins.plugins.datadog.DatadogClient;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;

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
        return TimeUnit.SECONDS.toSeconds(10);
    }

    @Override
    protected void execute(TaskListener taskListener) throws IOException, InterruptedException {
        try {
            if (DatadogUtilities.isApiKeyNull()) {
                return;
            }
            logger.fine("execute called: Publishing counters");

            // Instantiate the Datadog Client
            DatadogClient client = DatadogUtilities.getDatadogDescriptor().leaseDatadogClient();
            client.flushCounters();
        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }
    }
}

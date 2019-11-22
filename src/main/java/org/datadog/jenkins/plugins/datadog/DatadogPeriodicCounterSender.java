package org.datadog.jenkins.plugins.datadog;

import hudson.Extension;
import hudson.model.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.datadog.jenkins.plugins.datadog.clients.ConcurrentMetricCounters;
import org.datadog.jenkins.plugins.datadog.clients.CounterMetric;

@Extension
public class DatadogPeriodicCounterSender extends PeriodicWork {

    private static final Logger logger = Logger.getLogger(DatadogPeriodicCounterSender.class.getName());

    @Override
    public long getRecurrencePeriod() {
        // run frequency - 10 seconds
        return TimeUnit.SECONDS.toSeconds(10);
    }

    @Override
    protected void doRun() throws Exception {
        try {
            if (DatadogUtilities.isApiKeyNull()) {
                return;
            }

            // Instantiate the Datadog Client
            DatadogClient client = DatadogUtilities.getDatadogDescriptor().leaseDatadogClient();
            client.flushCounters();
        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }

    }
}

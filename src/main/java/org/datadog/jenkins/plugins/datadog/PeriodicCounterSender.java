package org.datadog.jenkins.plugins.datadog;

import hudson.Extension;
import hudson.model.*;
import org.datadog.jenkins.plugins.datadog.clients.DatadogHttpClient;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import org.datadog.jenkins.plugins.datadog.model.ConcurrentMetricCounters;

@Extension
public class PeriodicCounterSender extends PeriodicWork {

    private static final Logger logger = Logger.getLogger(PeriodicCounterSender.class.getName());

    @Override
    public long getRecurrencePeriod() {
        // run frequency - 15 seconds
        return PeriodicWork.MIN * 60 / 4;
    }

    @Override
    protected void doRun() throws Exception {
        if (DatadogUtilities.isApiKeyNull()) {
            return;
        }

        final String completed = "completed";

        // Instantiate the Datadog Client
        DatadogClient client = DatadogUtilities.getDatadogDescriptor().leaseDatadogClient();

        ConcurrentMap<String, Integer> cache = ConcurrentMetricCounters.Counters.get();
        ConcurrentMetricCounters.Counters.set(cache);

        for (String tags: cache.keySet()) {
            int counter = cache.get(tags);

            ((DatadogHttpClient) client).gauge("jenkins.job." + completed,
                    counter,
                    DatadogUtilities.getHostname(null),
                    tags);
        }

    }
}

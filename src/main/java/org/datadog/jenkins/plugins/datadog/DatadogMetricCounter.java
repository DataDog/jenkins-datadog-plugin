package org.datadog.jenkins.plugins.datadog;

import hudson.Extension;
import hudson.model.*;
import org.datadog.jenkins.plugins.datadog.clients.DatadogHttpClient;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import org.datadog.jenkins.plugins.datadog.model.LocalCacheCounters;

@Extension
public class DatadogMetricCounter extends PeriodicWork {

    private static final Logger logger = Logger.getLogger(DatadogMetricCounter.class.getName());

    @Override
    public long getRecurrencePeriod() {
        // run frequency - 60 seconds
        return PeriodicWork.MIN;
    }

    @Override
    protected void doRun() throws Exception {
        if (DatadogUtilities.isApiKeyNull()) {
            return;
        }

        // Instantiate the Datadog Client
        DatadogClient client = DatadogUtilities.getDatadogDescriptor().leaseDatadogClient();

        ConcurrentMap<String, Integer> cache = LocalCacheCounters.Cache.get();

        for (String tags: cache.keySet()) {
            int counter = cache.get(tags);

            ((DatadogHttpClient) client).gauge("jenkins.job.completed",
                    counter,
                    DatadogUtilities.getHostname(null),
                    tags);
        }

        LocalCacheCounters.Cache.set(cache);
    }
}

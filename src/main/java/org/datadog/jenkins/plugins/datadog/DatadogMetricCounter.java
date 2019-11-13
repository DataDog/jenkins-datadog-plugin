package org.datadog.jenkins.plugins.datadog;

import hudson.Extension;
import hudson.model.*;
import net.sf.json.JSONArray;
import org.datadog.jenkins.plugins.datadog.model.BuildData;

import javax.annotation.Nonnull;
import javax.print.attribute.standard.JobName;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.datadog.jenkins.plugins.datadog.model.LocalCacheCounters;


public class DatadogMetricCounter extends PeriodicWork {

    @Override
    public long getRecurrencePeriod() {
        // run frequency - 15 seconds
        return PeriodicWork.MIN / 60 * 15;
    }

    @Override
    protected void doRun() throws Exception {
        if (DatadogUtilities.isApiKeyNull()) {
            return;
        }
        //logger.fine("doRun called: Computing queue metrics");

        // Instantiate the Datadog Client
        DatadogClient client = DatadogUtilities.getDatadogDescriptor().leaseDatadogClient();

        // Test with list of strings for tags instead of using a Map
        // Map<String[], Integer> localCache = LocalCacheCounters.Cache.get();

        Map<Map<String, String>, Integer> localCache = LocalCacheCounters.Cache.get();

        Map<Map<String, String>, Integer> cache = LocalCacheCounters.deepCopy(localCache); // to be implemented
        LocalCacheCounters.Cache.set(new HashMap<>());

        for (Map<String,String> tags: cache.keySet()) {
            int counter = cache.get(tags);

            JSONArray tagsAsJSON = new JSONArray();

            for (Map.Entry entry : tags.entrySet()) {
                tagsAsJSON.add(String.format("%s:%s", entry.getKey(), entry.getValue()));
            }

            client.gauge("jenkins.job.completed",
                    counter,
                    DatadogUtilities.getHostname(null),
                    tagsAsJSON);
        }
    }
}

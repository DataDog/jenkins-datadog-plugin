package org.datadog.jenkins.plugins.datadog;

import hudson.Extension;
import hudson.model.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import org.datadog.jenkins.plugins.datadog.model.ConcurrentMetricCounters;
import org.datadog.jenkins.plugins.datadog.model.CounterMetric;

@Extension
public class DatadogPeriodicCounterSender extends PeriodicWork {

    private static final Logger logger = Logger.getLogger(DatadogPeriodicCounterSender.class.getName());

    @Override
    public long getRecurrencePeriod() {
        // run frequency - 15 seconds
        return PeriodicWork.MIN / 4;
    }

    @Override
    protected void doRun() throws Exception {
        if (DatadogUtilities.isApiKeyNull()) {
            return;
        }

        // Instantiate the Datadog Client
        DatadogClient client = DatadogUtilities.getDatadogDescriptor().leaseDatadogClient();

        ConcurrentMap<CounterMetric, Integer> counters = ConcurrentMetricCounters.Counters.get();

        ConcurrentMetricCounters.Counters.set(new ConcurrentHashMap<CounterMetric, Integer>());

        for (CounterMetric counterMetric: counters.keySet()) {
            int count = counters.get(counterMetric);

            client.gauge(counterMetric.getMetricName(),
                    count,
                    DatadogUtilities.getHostname(null),
                    counterMetric.getTags());
        }
    }
}

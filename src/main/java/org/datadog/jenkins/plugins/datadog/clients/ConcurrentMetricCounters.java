package org.datadog.jenkins.plugins.datadog.clients;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentMetricCounters {
    private static final ConcurrentMap<CounterMetric, Integer> metrics = new ConcurrentHashMap<>();
    public static final ThreadLocal<ConcurrentMap<CounterMetric, Integer>> Counters =
            new ThreadLocal<ConcurrentMap<CounterMetric, Integer>>() {
                @Override protected ConcurrentMap<CounterMetric, Integer> initialValue() {
                    return metrics;
                }
            };
}

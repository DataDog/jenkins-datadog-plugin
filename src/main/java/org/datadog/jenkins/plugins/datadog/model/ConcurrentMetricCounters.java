package org.datadog.jenkins.plugins.datadog.model;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentMetricCounters {
    private static final ConcurrentMap<String, Integer> metrics = new ConcurrentHashMap<String, Integer>();
    public static final ThreadLocal<ConcurrentMap<String, Integer>> Counters =
            new ThreadLocal<ConcurrentMap<String, Integer>>() {
                @Override protected ConcurrentMap<String, Integer> initialValue() {
                    return metrics;
                }
            };
}

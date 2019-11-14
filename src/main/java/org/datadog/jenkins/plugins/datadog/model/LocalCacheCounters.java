package org.datadog.jenkins.plugins.datadog.model;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LocalCacheCounters {
    private static final ConcurrentMap<String, Integer> counters = new ConcurrentHashMap<String, Integer>();
    public static final ThreadLocal<ConcurrentMap<String, Integer>> Cache =
            new ThreadLocal<ConcurrentMap<String, Integer>>() {
                @Override protected ConcurrentMap<String, Integer> initialValue() {
                    return counters;
                }
            };
}

package org.datadog.jenkins.plugins.datadog.model;

import org.datadog.jenkins.plugins.datadog.DatadogUtilities;

import java.util.HashMap;
import java.util.Map;

public class LocalCacheCounters {
    Map<Map<String, String>, Integer> counters = new HashMap<Map<String, String>, Integer>();
    public static final ThreadLocal<Map<Map<String, String>, Integer>> Cache =
            new ThreadLocal<Map<Map<String, String>, Integer>>() {
                @Override protected Map<Map<String, String>, Integer> initialValue() {
                    return new HashMap<Map<String, String>, Integer>();
                }
            };

    // Alternative implementation with a Map of List of strings, Integer
    /*
    Map<String[], Integer> countersAlt = new HashMap<String[], Integer>();
    public static final ThreadLocal<Map<String[], Integer>, Integer CacheTest =
            new ThreadLocal<Map<String[], Integer>>() {
                @Override protected Map<String[], Integer> initialValueTest() {
                    return new HashMap<String[], Integer>();
                }
            };
     */

    public static final Map<Map<String, String>, Integer> deepCopy(Map<Map<String, String>, Integer> localCache) {
        Map<Map<String, String>, Integer> copiedCache = localCache.putAll();
        return copiedCache;
    }
}

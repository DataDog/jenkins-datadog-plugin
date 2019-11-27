package org.datadog.jenkins.plugins.datadog.clients;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentMetricCounters {
    private static ConcurrentMetricCounters instance;
    private static ConcurrentMap<CounterMetric, Integer> counters = new ConcurrentHashMap<>();

    private ConcurrentMetricCounters(){}

    public static ConcurrentMetricCounters getInstance(){
        if(instance == null){
            synchronized (ConcurrentMetricCounters.class) {
                if(instance == null){
                    instance = new ConcurrentMetricCounters();
                }
            }
        }
        return instance;
    }

    public static ConcurrentMap<CounterMetric, Integer> get(){
        ConcurrentMetricCounters countersInstance = ConcurrentMetricCounters.getInstance();
        return countersInstance.getCounters();
    }

    public static void reset(){
        ConcurrentMetricCounters countersInstance = ConcurrentMetricCounters.getInstance();
        countersInstance.resetCounters();
    }

    private void resetCounters(){
        counters = new ConcurrentHashMap<>();
    }

    private ConcurrentMap<CounterMetric, Integer> getCounters(){
        return counters;
    }
}

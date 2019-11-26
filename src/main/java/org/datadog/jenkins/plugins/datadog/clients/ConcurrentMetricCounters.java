package org.datadog.jenkins.plugins.datadog.clients;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentMetricCounters {
    private static ConcurrentMetricCounters instance;
    private static ConcurrentMap<CounterMetric, Integer> metrics = new ConcurrentHashMap<>();
    public static ReentrantLock lock = new ReentrantLock();

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

    public void reset(){
        metrics = new ConcurrentHashMap<>();
    }

    public ConcurrentMap<CounterMetric, Integer> get(){
        return metrics;
    }
}

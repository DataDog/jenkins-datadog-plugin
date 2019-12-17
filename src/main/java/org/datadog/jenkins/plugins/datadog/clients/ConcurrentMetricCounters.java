package org.datadog.jenkins.plugins.datadog.clients;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

public class ConcurrentMetricCounters {

    private static final Logger logger = Logger.getLogger(ConcurrentMetricCounters.class.getName());
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

    private static ConcurrentMap<CounterMetric, Integer> get(){
        ConcurrentMetricCounters countersInstance = ConcurrentMetricCounters.getInstance();
        return countersInstance.getCounters();
    }

    private static void reset(){
        ConcurrentMetricCounters countersInstance = ConcurrentMetricCounters.getInstance();
        countersInstance.resetCounters();
    }

    private void resetCounters(){
        counters = new ConcurrentHashMap<>();
    }

    private ConcurrentMap<CounterMetric, Integer> getCounters(){
        return counters;
    }

    public synchronized void increment(String name, String hostname, Map<String, Set<String>> tags) {
        ConcurrentMap<CounterMetric, Integer> counters = ConcurrentMetricCounters.get();
        CounterMetric counterMetric = new CounterMetric(tags, name, hostname);
        Integer previousValue = counters.putIfAbsent(counterMetric, 1);
        if (previousValue != null){
            boolean ok = counters.replace(counterMetric, previousValue, previousValue + 1);
            // NOTE:
            // This while loop below should never be called since we are using a lock when flushing and
            // incrementing counters.
            while(!ok) {
                logger.warning("Couldn't increment counter " + name + " with value " + (previousValue + 1) +
                        " previousValue = " + previousValue);
                previousValue = counters.get(counterMetric);
                ok = counters.replace(counterMetric, previousValue, previousValue + 1);
            }
        }
        previousValue = previousValue == null ? 0 : previousValue;
        logger.fine("Counter " + name + " updated from previousValue " + previousValue + " to "
                + (previousValue + 1));
    }

    public synchronized ConcurrentMap<CounterMetric, Integer> getAndReset(){
        ConcurrentMap<CounterMetric, Integer> counters = ConcurrentMetricCounters.get();
        ConcurrentMetricCounters.reset();
        return counters;
    }

}

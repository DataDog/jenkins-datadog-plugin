/*
The MIT License

Copyright (c) 2015-Present Datadog, Inc <opensource@datadoghq.com>
All rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

package org.datadog.jenkins.plugins.datadog.clients;

import org.datadog.jenkins.plugins.datadog.util.SuppressFBWarnings;

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

    @SuppressFBWarnings(value="DC_DOUBLECHECK")
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

    @SuppressFBWarnings(value="ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
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
        previousValue = (previousValue == null) ? Integer.valueOf(0) : previousValue;
        logger.fine("Counter " + name + " updated from previousValue " + previousValue + " to "
                + (previousValue + Integer.valueOf(1)));
    }

    public synchronized ConcurrentMap<CounterMetric, Integer> getAndReset(){
        ConcurrentMap<CounterMetric, Integer> counters = ConcurrentMetricCounters.get();
        ConcurrentMetricCounters.reset();
        return counters;
    }

}

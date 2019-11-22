package org.datadog.jenkins.plugins.datadog.clients;

import net.sf.json.JSONArray;
import org.datadog.jenkins.plugins.datadog.DatadogClient;
import org.junit.Assert;
import org.junit.Test;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DatadogClientTest {

    @Test
    public void testIncrementCountAndFlush() throws IOException, InterruptedException {
        DatadogClient client = new DatadogHttpClient();
        JSONArray tags1 = new JSONArray();
        tags1.add("tag1");
        tags1.add("tag2");
        client.incrementCounter("metric1", "host1", tags1);
        client.incrementCounter("metric1", "host1", tags1);

        JSONArray tags2 = new JSONArray();
        tags2.add("tag1");
        tags2.add("tag2");
        tags2.add("tag3");
        client.incrementCounter("metric1", "host1", tags2);

        client.incrementCounter("metric1", "host2", tags2);
        client.incrementCounter("metric1", "host2", tags2);

        client.incrementCounter("metric2", "host2", tags2);

        // The following code should be the same as in the flushCounters method
        ConcurrentMap<CounterMetric, Integer> counters = ConcurrentMetricCounters.Counters.get();
        ConcurrentMetricCounters.Counters.set(new ConcurrentHashMap<CounterMetric, Integer>());

        // Check counter is reset as expected
        ConcurrentMap<CounterMetric, Integer> countersEmpty = ConcurrentMetricCounters.Counters.get();
        Assert.assertTrue(countersEmpty.size() == 0);

        // Check that metrics to submit are correct
        boolean check1  = false, check2 = false, check3 = false, check4 = false;
        Assert.assertTrue("counters = " + counters.size(), counters.size() == 4);
        for (CounterMetric counterMetric: counters.keySet()) {
            int count = counters.get(counterMetric);
            if(counterMetric.getMetricName().equals("metric1") && counterMetric.getHostname().equals("host1")
                    && counterMetric.getTags().size() == 2){
                Assert.assertTrue(count == 2);
                check1 = true;
            } else if (counterMetric.getMetricName().equals("metric1") && counterMetric.getHostname().equals("host1")
                    && counterMetric.getTags().size() == 3){
                Assert.assertTrue(count == 1);
                check2 = true;
            } else if (counterMetric.getMetricName().equals("metric1") && counterMetric.getHostname().equals("host2")
                    && counterMetric.getTags().size() == 3){
                Assert.assertTrue(count == 2);
                check3 = true;
            } else if (counterMetric.getMetricName().equals("metric2") && counterMetric.getHostname().equals("host2")
                    && counterMetric.getTags().size() == 3){
                Assert.assertTrue(count == 1);
                check4 = true;
            }
        }
        Assert.assertTrue(check1 + " " + check2 + " " + check3 + " " + check4,
                check1 && check2 && check3 && check4);
    }

}

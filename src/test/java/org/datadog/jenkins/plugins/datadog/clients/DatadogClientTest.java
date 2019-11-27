package org.datadog.jenkins.plugins.datadog.clients;

import net.sf.json.JSONArray;
import org.datadog.jenkins.plugins.datadog.DatadogClient;
import org.junit.Assert;
import org.junit.Test;
import java.io.IOException;
import java.util.concurrent.*;

public class DatadogClientTest {

    @Test
    public void testIncrementCountAndFlush() throws IOException, InterruptedException {
        DatadogClient client = DatadogHttpClient.getInstance(null, null);
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
        ConcurrentMap<CounterMetric, Integer> counters = ConcurrentMetricCounters.getInstance().getAndReset();

        // Check counter is reset as expected
        ConcurrentMap<CounterMetric, Integer> countersEmpty = ConcurrentMetricCounters.getInstance().getAndReset();
        Assert.assertTrue("size = " + countersEmpty.size(), countersEmpty.size() == 0);

        // Check that metrics to submit are correct
        boolean check1  = false, check2 = false, check3 = false, check4 = false;
        Assert.assertTrue("counters = " + counters.size(), counters.size() == 4);
        for (CounterMetric counterMetric: counters.keySet()) {
            int count = counters.get(counterMetric);
            if(counterMetric.getMetricName().equals("metric1") && counterMetric.getHostname().equals("host1")
                    && counterMetric.getTags().size() == 2){
                Assert.assertTrue("count = " + count, count == 2);
                check1 = true;
            } else if (counterMetric.getMetricName().equals("metric1") && counterMetric.getHostname().equals("host1")
                    && counterMetric.getTags().size() == 3){
                Assert.assertTrue("count = " + count,count == 1);
                check2 = true;
            } else if (counterMetric.getMetricName().equals("metric1") && counterMetric.getHostname().equals("host2")
                    && counterMetric.getTags().size() == 3){
                Assert.assertTrue("count = " + count,count == 2);
                check3 = true;
            } else if (counterMetric.getMetricName().equals("metric2") && counterMetric.getHostname().equals("host2")
                    && counterMetric.getTags().size() == 3){
                Assert.assertTrue("count = " + count,count == 1);
                check4 = true;
            }
        }
        Assert.assertTrue(check1 + " " + check2 + " " + check3 + " " + check4,
                check1 && check2 && check3 && check4);
    }

    @Test
    public void testIncrementCountAndFlushThreadedEnv() throws IOException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Runnable increment = new Runnable() {
            @Override
            public void run() {
                // We use a new instance of a client on every run.
                DatadogClient client = DatadogHttpClient.getInstance(null, null);
                JSONArray tags = new JSONArray();
                tags.add("tag1");
                tags.add("tag2");
                client.incrementCounter("metric1", "host1", tags);
            }
        };

        for(int i = 0; i < 10000; i++){
            executor.submit(increment);
        }

        stop(executor);

        // Check counter is reset as expected
        ConcurrentMap<CounterMetric, Integer> counters = ConcurrentMetricCounters.getInstance().getAndReset();
        Assert.assertTrue("size = " + counters.size(), counters.size() == 1);
        Assert.assertTrue("counters.values() = " + counters.values(), counters.values().contains(10000));

    }

    @Test
    public void testIncrementCountAndFlushThreadedEnvThreadCheck() throws IOException, InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Runnable increment = new Runnable() {
            @Override
            public void run() {
                // We use a new instance of a client on every run.
                DatadogClient client = DatadogHttpClient.getInstance(null, null);
                JSONArray tags = new JSONArray();
                tags.add("tag1");
                tags.add("tag2");
                client.incrementCounter("metric1", "host1", tags);
            }
        };

        for(int i = 0; i < 10000; i++){
            executor.submit(increment);
        }

        stop(executor);

        // We also check the result in a distinct thread
        ExecutorService single = Executors.newSingleThreadExecutor();
        Callable<Boolean> check = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                // Check counter is reset as expected
                ConcurrentMap<CounterMetric, Integer> counters = ConcurrentMetricCounters.getInstance().getAndReset();
                Assert.assertTrue("size = " + counters.size(), counters.size() == 1);
                Assert.assertTrue("counters.values() = " + counters.values(), counters.values().contains(10000));
                return true;
            }
        };

        Future<Boolean> value = single.submit(check);

        stop(single);

        Assert.assertTrue(value.get());

    }

    @Test
    public void testIncrementCountAndFlushThreadedEnvOneClient() throws IOException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        // We only have one instance of the client used by all threads
        final DatadogClient client = DatadogHttpClient.getInstance(null, null);
        Runnable increment = new Runnable() {
            @Override
            public void run() {
                JSONArray tags = new JSONArray();
                tags.add("tag1");
                tags.add("tag2");
                client.incrementCounter("metric1", "host1", tags);
            }
        };

        for(int i = 0; i < 10000; i++){
            executor.submit(increment);
        }

        stop(executor);

        // Check counter is reset as expected
        ConcurrentMap<CounterMetric, Integer> counters = ConcurrentMetricCounters.getInstance().getAndReset();
        Assert.assertTrue("size = " + counters.size(), counters.size() == 1);
        Assert.assertTrue("counters.values() = " + counters.values(), counters.values().contains(10000));

    }

    private static void stop(ExecutorService executor) {
        try {
            executor.shutdown();
            executor.awaitTermination(3, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            System.err.println("termination interrupted");
        }
        finally {
            if (!executor.isTerminated()) {
                System.err.println("killing non-finished tasks");
            }
            executor.shutdownNow();
        }
    }

}

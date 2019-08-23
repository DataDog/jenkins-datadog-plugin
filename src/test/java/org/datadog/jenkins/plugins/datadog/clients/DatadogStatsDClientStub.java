package org.datadog.jenkins.plugins.datadog.clients;

import com.timgroup.statsd.ServiceCheck;
import com.timgroup.statsd.StatsDClient;
import net.sf.json.JSONArray;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class DatadogStatsDClientStub implements StatsDClient {

    List<DatadogMetric> metrics = new ArrayList<>();
    List<DatadogMetric> counters = new ArrayList<>();

    @Override
    public void incrementCounter(String s, String... strings) {
        JSONArray jtags = new JSONArray();
        if (strings != null) {
            jtags.addAll(Arrays.asList(strings));
        }
        boolean exists = false;
        for (DatadogMetric m : this.metrics) {
            if (Objects.equals(m.getName(), s) && Objects.equals(m.getHostname(), "") &&
                    Objects.equals(m.getTags(), jtags)) {
                double value = m.getValue();
                this.metrics.remove(m);
                this.metrics.add(new DatadogMetric(s, value + 1, "", jtags));
                exists = true;
                break;
            }
        }
        if (!exists) {
            this.metrics.add(new DatadogMetric(s, 1, "", jtags));
        }
    }

    @Override
    public void gauge(String s, double v, String... strings) {
        JSONArray jtags = new JSONArray();
        if (strings != null) {
            jtags.addAll(Arrays.asList(strings));
        }
        this.metrics.add(new DatadogMetric(s, v, "", jtags));
    }

    @Override
    public void gauge(String s, long l, String... strings) {
        gauge(s, Double.valueOf(l), strings);
    }

    public boolean assertMetric(String name, long value, String[] tags) {
        JSONArray jtags = new JSONArray();
        if (tags != null) {
            jtags.addAll(Arrays.asList(tags));
        }
        DatadogMetric m = new DatadogMetric(name, value, "", jtags);
        if (this.metrics.contains(m)) {
            this.metrics.remove(m);
            return true;
        }
        Assert.fail("metric { " + m.toString() + " does not exist. " +
                "metrics: {" + this.metrics.toString() + " }");
        return false;
    }

//    public boolean assertCounter(String name, long value, String[] tags) {
//        JSONArray jtags = new JSONArray();
//        if (tags != null) {
//            jtags.addAll(Arrays.asList(tags));
//        }
//        DatadogMetric m = new DatadogMetric(name, value, "", jtags);
//        if (this.counters.contains(m)) {
//            this.counters.remove(m);
//            return true;
//        }
//        Assert.fail("counter { " + m.toString() + " does not exist. " +
//                "counters: {" + this.counters.toString() + " }");
//        return false;
//    }

    public boolean assertedAllMetricsAndCounters() {
        if (this.metrics.size() == 0 && this.counters.size() == 0) {
            return true;
        }

        Assert.fail("metrics: {" + this.metrics.toString() + " }, counters : {" +
                this.counters.toString() + "}");
        return false;
    }

    @Override
    public void stop() {
        // NO-OP
    }

    @Override
    public void count(String s, long l, String... strings) {
        // NO-OP
    }

    @Override
    public void increment(String s, String... strings) {
        // NO-OP
    }

    @Override
    public void recordGaugeValue(String s, long l, String... strings) {
        // NO-OP
    }

    @Override
    public void decrementCounter(String s, String... strings) {
        // NO-OP
    }

    @Override
    public void decrement(String s, String... strings) {
        // NO-OP
    }

    @Override
    public void recordGaugeValue(String s, double v, String... strings) {
        // NO-OP
    }

    @Override
    public void recordExecutionTime(String s, long l, String... strings) {
        // NO-OP
    }

    @Override
    public void time(String s, long l, String... strings) {
        // NO-OP
    }

    @Override
    public void recordHistogramValue(String s, double v, String... strings) {
        // NO-OP
    }

    @Override
    public void histogram(String s, double v, String... strings) {
        // NO-OP
    }

    @Override
    public void recordHistogramValue(String s, long l, String... strings) {
        // NO-OP
    }

    @Override
    public void histogram(String s, long l, String... strings) {
        // NO-OP
    }

    @Override
    public void recordServiceCheckRun(ServiceCheck serviceCheck) {
        // NO-OP
    }

    @Override
    public void serviceCheck(ServiceCheck serviceCheck) {
        // NO-OP
    }
}

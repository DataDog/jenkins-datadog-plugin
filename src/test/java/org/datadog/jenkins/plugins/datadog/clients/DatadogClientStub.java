package org.datadog.jenkins.plugins.datadog.clients;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogClient;
import org.junit.Assert;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class DatadogClientStub implements DatadogClient {

    List<DatadogMetric> metrics = new ArrayList<>();
    List<DatadogMetric> serviceChecks = new ArrayList<>();

    public DatadogClientStub() {
        this.metrics = new ArrayList<>();
        this.serviceChecks = new ArrayList<>();
    }

    @Override
    public boolean sendEvent(JSONObject payload) {
        //NO-OP
        return true;
    }

    @Override
    public boolean gauge(String name, long value, String hostname, JSONArray tags) {
        this.metrics.add(new DatadogMetric(name, value, hostname, tags));
        return true;
    }

    @Override
    public boolean serviceCheck(String name, int code, String hostname, JSONArray tags) {
        this.serviceChecks.add(new DatadogMetric(name, code, hostname, tags));
        return true;
    }

    @Override
    public boolean validate() throws IOException, ServletException {
        return true;
    }

    @Override
    public boolean sendLogs(JSONObject payloadLogs) throws IOException {
        return false;
    }

    public boolean assertMetric(String name, double value, String hostname, String[] tags) {
        JSONArray jtags = new JSONArray();
        if (tags != null) {
            jtags.addAll(Arrays.asList(tags));
        }
        DatadogMetric m = new DatadogMetric(name, value, hostname, jtags);
        if (this.metrics.contains(m)) {
            this.metrics.remove(m);
            return true;
        }
        Assert.fail("metric { " + m.toString() + " does not exist. " +
                "metrics: {" + this.metrics.toString() + " }");
        return false;
    }

//    public boolean assertMetricNotZero(String name, String hostname, String[] tags) {
//        JSONArray jtags = new JSONArray();
//        if (tags != null) {
//            jtags.addAll(Arrays.asList(tags));
//        }
//        for (DatadogMetric m : this.metrics) {
//            if (Objects.equals(m.getName(), name) && Objects.equals(m.getHostname(), hostname) &&
//                    Objects.equals(m.getTags(), jtags)) {
//                DatadogMetric r = new DatadogMetric(name, m.getValue(), hostname, jtags);
//                this.metrics.remove(r);
//                return true;
//            }
//        }
//        Assert.fail("metric with {name: " + name + ", hostname: " + hostname + ", tags: " + tags.toString() +
//                "} does not exist. metrics: {" + this.metrics.toString() + " }");
//        return false;
//    }

    public boolean assertServiceCheck(String name, int code, String hostname, String[] tags) {
        JSONArray jtags = new JSONArray();
        if (tags != null) {
            jtags.addAll(Arrays.asList(tags));
        }
        DatadogMetric m = new DatadogMetric(name, code, hostname, jtags);
        if (this.serviceChecks.contains(m)) {
            this.serviceChecks.remove(m);
            return true;
        }
        Assert.fail("serviceCheck { " + m.toString() + " does not exist. " +
                "serviceChecks: {" + this.serviceChecks.toString() + "}");
        return false;
    }

    public boolean assertedAllMetricsAndServiceChecks() {
        if (this.metrics.size() == 0 && this.serviceChecks.size() == 0) {
            return true;
        }

        Assert.fail("metrics: {" + this.metrics.toString() + " }, serviceChecks : {" +
                this.serviceChecks.toString() + "}");
        return false;
    }
}

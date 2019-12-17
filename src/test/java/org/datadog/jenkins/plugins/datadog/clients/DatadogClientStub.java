package org.datadog.jenkins.plugins.datadog.clients;

import hudson.util.Secret;
import org.datadog.jenkins.plugins.datadog.DatadogClient;
import org.datadog.jenkins.plugins.datadog.logs.LogSender;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.junit.Assert;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;

public class DatadogClientStub implements DatadogClient {

    List<DatadogMetric> metrics = new ArrayList<>();
    List<DatadogMetric> serviceChecks = new ArrayList<>();

    public DatadogClientStub() {
        this.metrics = new ArrayList<>();
        this.serviceChecks = new ArrayList<>();
    }

    @Override
    public void setUrl(String url) {
        // noop
    }

    @Override
    public void setApiKey(Secret apiKey) {
        // noop
    }

    @Override
    public void setHostname(String hostname) {
        // noop
    }

    @Override
    public void setPort(int port) {
        // noop
    }

    @Override
    public boolean event(DatadogEvent event) {
        //NO-OP
        return true;
    }

    @Override
    public void incrementCounter(String name, String hostname, Map<String, Set<String>> tags) {
        for (DatadogMetric m : this.metrics) {
            if(m.same(new DatadogMetric(name, 0, hostname, convertTagMapToList(tags)))) {
                double value = m.getValue() + 1;
                this.metrics.remove(m);
                this.metrics.add(new DatadogMetric(name, value, hostname, convertTagMapToList(tags)));
                return;
            }
        }
        this.metrics.add(new DatadogMetric(name, 1, hostname, convertTagMapToList(tags)));
    }

    @Override
    public void flushCounters() {
        // noop
    }

    @Override
    public boolean gauge(String name, long value, String hostname, Map<String, Set<String>> tags) {
        this.metrics.add(new DatadogMetric(name, value, hostname, convertTagMapToList(tags)));
        return true;
    }

    @Override
    public boolean serviceCheck(String name, Status status, String hostname, Map<String, Set<String>> tags) {
        this.serviceChecks.add(new DatadogMetric(name, status.toValue(), hostname, convertTagMapToList(tags)));
        return true;
    }

    @Override
    public boolean validate() throws IOException, ServletException {
        return true;
    }

    @Override
    public boolean sendLogs(LogSender payloadLogs) throws IOException {
        return false;
    }

    public boolean assertMetric(String name, double value, String hostname, String[] tags) {
        DatadogMetric m = new DatadogMetric(name, value, hostname, Arrays.asList(tags));
        if (this.metrics.contains(m)) {
            this.metrics.remove(m);
            return true;
        }
        Assert.fail("metric { " + m.toString() + " does not exist. " +
                "metrics: {" + this.metrics.toString() + " }");
        return false;
    }

    public boolean assertServiceCheck(String name, int code, String hostname, String[] tags) {
        DatadogMetric m = new DatadogMetric(name, code, hostname, Arrays.asList(tags));
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

    public static List<String> convertTagMapToList(Map<String, Set<String>> tags){
        List<String> result = new ArrayList<>();
        for (String name : tags.keySet()) {
            Set<String> values = tags.get(name);
            for (String value : values){
                result.add(String.format("%s:%s", name, value));
            }
        }
        return result;

    }

    public static Map<String, Set<String>> addTagToMap(Map<String, Set<String>> tags, String name, String value){
        Set<String> v = tags.containsKey(name) ? tags.get(name) : new HashSet<String>();
        v.add(value);
        tags.put(name, v);
        return tags;
    }
}

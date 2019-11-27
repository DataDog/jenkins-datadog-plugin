package org.datadog.jenkins.plugins.datadog.clients;

import net.sf.json.JSONArray;

public class CounterMetric {
    private JSONArray tags = new JSONArray();
    private String metricName;
    private String hostname;

    public CounterMetric(JSONArray tags, String metricName, String hostname) {
        this.tags = tags;
        this.metricName = metricName;
        this.hostname = hostname;
    }

    public JSONArray getTags() {
        return tags;
    }

    public void setTags(JSONArray tags) {
        this.tags = tags;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CounterMetric)) return false;

        CounterMetric that = (CounterMetric) o;

        if (tags != null ? !tags.equals(that.tags) : that.tags != null) return false;
        if (metricName != null ? !metricName.equals(that.metricName) : that.metricName != null) return false;
        return hostname != null ? hostname.equals(that.hostname) : that.hostname == null;
    }

    @Override
    public int hashCode() {
        int result = tags != null ? tags.hashCode() : 0;
        result = 31 * result + (metricName != null ? metricName.hashCode() : 0);
        result = 31 * result + (hostname != null ? hostname.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CounterMetric{" +
                "tags=" + tags +
                ", metricName='" + metricName + '\'' +
                ", hostname='" + hostname + '\'' +
                '}';
    }
}

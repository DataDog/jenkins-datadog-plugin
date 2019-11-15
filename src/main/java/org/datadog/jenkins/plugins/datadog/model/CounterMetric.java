package org.datadog.jenkins.plugins.datadog.model;

import net.sf.json.JSONArray;

import java.util.Objects;

public class CounterMetric {
    JSONArray tags = new JSONArray();
    String metricName;

    public CounterMetric(JSONArray tags, String metricName) {
        this.tags = tags;
        this.metricName = metricName;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CounterMetric)) return false;
        CounterMetric that = (CounterMetric) o;
        return Objects.equals(tags, that.tags) &&
                metricName.equals(that.metricName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tags, metricName);
    }

    @Override
    public String toString() {
        return "MetricName{" +
                "tags=" + tags +
                ", metricName='" + metricName + '\'' +
                '}';
    }
}

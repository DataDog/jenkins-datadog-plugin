package org.datadog.jenkins.plugins.datadog.clients;

import net.sf.json.JSONArray;

public class DatadogMetric {
    private String name = null;
    private double value;
    private String hostname = null;
    private JSONArray tags = null;

    DatadogMetric(String name, double value, String hostname, JSONArray tags) {
        this.name = name;
        this.value = value;
        this.hostname = hostname;
        this.tags = tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DatadogMetric)) return false;

        DatadogMetric that = (DatadogMetric) o;

        if (Double.compare(that.value, value) != 0) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (hostname != null ? !hostname.equals(that.hostname) : that.hostname != null) return false;
        return tags != null ? tags.equals(that.tags) : that.tags == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = name != null ? name.hashCode() : 0;
        temp = Double.doubleToLongBits(value);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (hostname != null ? hostname.hashCode() : 0);
        result = 31 * result + (tags != null ? tags.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Metric{" +
                "name='" + name + '\'' +
                ", value=" + value +
                ", hostname='" + hostname + '\'' +
                ", tags=" + tags +
                '}';
    }

    public String getName() {
        return name;
    }

    public double getValue() {
        return value;
    }

    public String getHostname() {
        return hostname;
    }

    public JSONArray getTags() {
        return tags;
    }
}

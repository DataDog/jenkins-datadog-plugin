package org.datadog.jenkins.plugins.datadog.clients;

import java.util.Collections;
import java.util.List;

public class DatadogMetric {
    private String name = null;
    private double value;
    private String hostname = null;
    private List<String> tags = null;

    DatadogMetric(String name, double value, String hostname, List<String> tags) {
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
        return this.same(that);
    }

    /**
     * This method check that this object and the one passed as parameter are equal to the exception of the value field.
     * @param m DatadogMetric object to compare to.
     * @return true is both DatadogMetric are the same otherwise false
     */
    public boolean same(DatadogMetric m) {
        if (this == m) return true;
        if (m == null) return false;
        if (name != null ? !name.equals(m.name) : m.name != null) return false;
        if (hostname != null ? !hostname.equals(m.hostname) : m.hostname != null) return false;
        if (tags != null && m.tags == null) return false;
        Collections.sort(tags);
        Collections.sort(m.tags);
        return tags != null ? tags.toString().equals(m.tags.toString()) : m.tags == null;
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

    public List<String> getTags() {
        return tags;
    }
}

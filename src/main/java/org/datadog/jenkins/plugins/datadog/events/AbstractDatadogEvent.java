package org.datadog.jenkins.plugins.datadog.events;

import org.datadog.jenkins.plugins.datadog.DatadogEvent;

import java.util.Map;
import java.util.Set;

public abstract class AbstractDatadogEvent implements DatadogEvent {

    private String title;
    private String text;
    private String host;
    private DatadogEvent.Priority priority;
    private DatadogEvent.AlertType alertType;
    private String aggregationKey;
    private Long date;
    private Map<String, Set<String>> tags;

    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public DatadogEvent.Priority getPriority() {
        return priority;
    }

    public void setPriority(DatadogEvent.Priority priority) {
        this.priority = priority;
    }

    @Override
    public DatadogEvent.AlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(DatadogEvent.AlertType alertType) {
        this.alertType = alertType;
    }

    @Override
    public String getAggregationKey() {
        return aggregationKey;
    }

    public void setAggregationKey(String aggregationKey) {
        this.aggregationKey = aggregationKey;
    }

    @Override
    public Long getDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    @Override
    public Map<String, Set<String>> getTags() {
        return tags;
    }

    public void setTags(Map<String, Set<String>> tags) {
        this.tags = tags;
    }
}

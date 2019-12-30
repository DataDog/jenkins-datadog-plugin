/*
The MIT License

Copyright (c) 2010-2020, Datadog <opensource@datadoghq.com>
All rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

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

package org.datadog.jenkins.plugins.datadog.listeners;

import hudson.model.Queue;
import org.datadog.jenkins.plugins.datadog.DatadogClient;

public class DatadogBuildListenerTestWrapper extends DatadogBuildListener {
    Queue queue;
    DatadogClient client;

    public void setQueue(Queue queue) {
        this.queue = queue;
    }

    public void setDatadogClient(DatadogClient client) {
        this.client = client;
    }

    public Queue getQueue(){
        return this.queue;
    }

    public DatadogClient getDatadogClient(){
        return this.client;
    }
}

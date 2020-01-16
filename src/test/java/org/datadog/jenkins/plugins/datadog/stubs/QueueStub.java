package org.datadog.jenkins.plugins.datadog.stubs;

import hudson.model.LoadBalancer;
import hudson.model.Queue;

import javax.annotation.Nonnull;

public class QueueStub extends Queue {

    public Queue.Item item;

    public QueueStub(@Nonnull LoadBalancer loadBalancer) {
        super(loadBalancer);
    }

    public void setItem(Queue.Item item){
        this.item = item;
    }

    public Queue.Item getItem(long id) {
        return this.item;
    }

}

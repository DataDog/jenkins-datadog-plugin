/*
The MIT License

Copyright (c) 2015-Present Datadog, Inc <opensource@datadoghq.com>
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

package org.datadog.jenkins.plugins.datadog.publishers;

import hudson.Extension;
import hudson.model.PeriodicWork;
import hudson.model.Queue;
import org.datadog.jenkins.plugins.datadog.DatadogClient;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.clients.ClientFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * This class registers a {@link PeriodicWork} with Jenkins to run periodically in order to enable
 * us to compute metrics related to the Jenkins queue.
 */
@Extension
public class DatadogQueuePublisher extends PeriodicWork {

    private static final Logger logger = Logger.getLogger(DatadogQueuePublisher.class.getName());

    private static final long RECURRENCE_PERIOD = TimeUnit.MINUTES.toMillis(1);
    private static final Queue queue = Queue.getInstance();

    @Override
    public long getRecurrencePeriod() {
        return RECURRENCE_PERIOD;
    }

    @Override
    protected void doRun() throws Exception {
        try {
            logger.fine("doRun called: Computing queue metrics");

            // Get Datadog Client Instance
            DatadogClient client = ClientFactory.getClient();
            Map<String, Set<String>> tags = DatadogUtilities.getTagsFromGlobalTags();

            long size = 0;
            long buildable = queue.countBuildableItems();
            long pending = queue.getPendingItems().size();
            long stuck = 0;
            long blocked = 0;
            final Queue.Item[] items = queue.getItems();
            for (Queue.Item item : items) {
                size++;
                if(item.isStuck()){
                    stuck++;
                }
                if(item.isBlocked()){
                    blocked++;
                }
            }
            String hostname = DatadogUtilities.getHostname("null");
            client.gauge("jenkins.queue.size", size, hostname, tags);
            client.gauge("jenkins.queue.buildable", buildable, hostname, tags);
            client.gauge("jenkins.queue.pending", pending, hostname, tags);
            client.gauge("jenkins.queue.stuck", stuck, hostname, tags);
            client.gauge("jenkins.queue.blocked", blocked, hostname, tags);

        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }

    }
}

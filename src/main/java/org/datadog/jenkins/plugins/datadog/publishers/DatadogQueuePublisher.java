package org.datadog.jenkins.plugins.datadog.publishers;

import hudson.Extension;
import hudson.model.PeriodicWork;
import hudson.model.Queue;
import org.datadog.jenkins.plugins.datadog.DatadogClient;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;

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
            DatadogClient client = DatadogUtilities.getDatadogClient();
            Map<String, Set<String>> tags = DatadogUtilities.getDatadogGlobalDescriptor().getGlobalTags();

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

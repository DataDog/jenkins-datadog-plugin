package org.datadog.jenkins.plugins.datadog;

import hudson.Extension;
import hudson.model.PeriodicWork;
import hudson.model.Queue;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * This class registers a {@link PeriodicWork} with Jenkins to run periodically in order to enable
 * us to compute metrics related to the size of the queue.
 */
@Extension
public class DatadogQueueListener extends PeriodicWork {
    private static final Logger logger = Logger.getLogger(DatadogQueueListener.class.getName());

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

            long size = queue.getApproximateItemsQuickly().size();
            String hostname = DatadogUtilities.getHostname("null");
            client.gauge("jenkins.queue.size", size, hostname, null);

        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }

    }
}

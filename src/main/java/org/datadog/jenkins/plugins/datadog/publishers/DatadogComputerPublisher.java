package org.datadog.jenkins.plugins.datadog.publishers;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.PeriodicWork;
import jenkins.model.Jenkins;
import org.datadog.jenkins.plugins.datadog.DatadogClient;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.util.TagsUtil;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * This class registers a {@link PeriodicWork} with Jenkins to run periodically in order to enable
 * us to compute metrics related to nodes and executors.
 */
@Extension
public class DatadogComputerPublisher extends PeriodicWork {

    private static final Logger logger = Logger.getLogger(DatadogComputerPublisher.class.getName());

    private static final long RECURRENCE_PERIOD = TimeUnit.MINUTES.toMillis(1);

    @Override
    public long getRecurrencePeriod() {
        return RECURRENCE_PERIOD;
    }

    @Override
    protected void doRun() throws Exception {
        try {
            logger.fine("doRun called: Computing Node metrics");

            // Get Datadog Client Instance
            DatadogClient client = DatadogUtilities.getDatadogClient();
            String hostname = DatadogUtilities.getHostname("null");

            long nodeCount = 0;
            long nodeOffline = 0;
            long nodeOnline = 0;
            Computer[] computers = Jenkins.getInstance().getComputers();
            final Map<String, Set<String>> globalTags = DatadogUtilities.getTagsFromGlobalTags();
            for (Computer computer : computers) {
                nodeCount++;
                if (computer.isOffline()) {
                    nodeOffline++;
                }
                if (computer.isOnline()) {
                    nodeOnline++;
                }

                int executorCount = computer.countExecutors();
                int inUse = computer.countBusy();
                int free = computer.countIdle();

                Map<String, Set<String>> tags = TagsUtil.merge(
                        DatadogUtilities.getComputerTags(computer), globalTags);
                client.gauge("jenkins.executor.count", executorCount, hostname, tags);
                client.gauge("jenkins.executor.in_use", inUse, hostname, tags);
                client.gauge("jenkins.executor.free", free, hostname, tags);
            }
            client.gauge("jenkins.node.count", nodeCount, hostname, globalTags);
            client.gauge("jenkins.node.offline", nodeOffline, hostname, globalTags);
            client.gauge("jenkins.node.online", nodeOnline, hostname, globalTags);

        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }

    }

}

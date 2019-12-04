package org.datadog.jenkins.plugins.datadog.publishers;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.PeriodicWork;
import hudson.model.labels.LabelAtom;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import org.datadog.jenkins.plugins.datadog.DatadogClient;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * This class registers a {@link PeriodicWork} with Jenkins to run periodically in order to enable
 * us to compute metrics related to nodes and executors.
 */
@Extension
public class DatadogNodePublisher extends PeriodicWork {

    private static final Logger logger = Logger.getLogger(DatadogNodePublisher.class.getName());

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
                Set<LabelAtom> labels = null;
                try {
                    labels = computer.getNode().getAssignedLabels();
                } catch (NullPointerException e){
                    logger.fine("Could not retrieve labels");
                }
                String nodeHostname = computer.getHostName();
                String nodeName;
                if (computer instanceof Jenkins.MasterComputer) {
                    nodeName =  "master";
                } else {
                    nodeName = computer.getName();
                }
                JSONArray tags = new JSONArray();
                tags.add("node-name:" + nodeName);
                if(nodeHostname != null){
                    tags.add("node-hostname:" + nodeHostname);
                }
                if(labels != null){
                    for (LabelAtom label: labels){
                        tags.add("node-label:" + label.getName());
                    }
                }
                client.gauge("jenkins.executor.count", executorCount, hostname, tags);
                client.gauge("jenkins.executor.in-use", inUse, hostname, tags);
                client.gauge("jenkins.executor.free", free, hostname, tags);
            }

            client.gauge("jenkins.node.count", nodeCount, hostname, null);
            client.gauge("jenkins.node.offline", nodeOffline, hostname, null);
            client.gauge("jenkins.node.online", nodeOnline, hostname, null);

        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }

    }

}

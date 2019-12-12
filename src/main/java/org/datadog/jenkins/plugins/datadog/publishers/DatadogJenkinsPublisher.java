package org.datadog.jenkins.plugins.datadog.publishers;

import hudson.Extension;
import hudson.model.PeriodicWork;
import hudson.model.Project;
import jenkins.model.Jenkins;
import org.datadog.jenkins.plugins.datadog.DatadogClient;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * This class registers a {@link PeriodicWork} with Jenkins to run periodically in order to enable
 * us to compute metrics related to Jenkins level metrics.
 */
@Extension
public class DatadogJenkinsPublisher extends PeriodicWork {

    private static final Logger logger = Logger.getLogger(DatadogJenkinsPublisher.class.getName());

    private static final long RECURRENCE_PERIOD = TimeUnit.MINUTES.toMillis(1);

    @Override
    public long getRecurrencePeriod() {
        return RECURRENCE_PERIOD;
    }

    @Override
    protected void doRun() throws Exception {
        try {
            logger.fine("doRun called: Computing Jenkins metrics");

            // Get Datadog Client Instance
            DatadogClient client = DatadogUtilities.getDatadogClient();
            String hostname = DatadogUtilities.getHostname("null");
            Map<String, Set<String>> tags = DatadogUtilities.getTagsFromGlobalTags();
            long projectCount = 0;
            try {
                projectCount = Jenkins.getInstance().getAllItems(Project.class).size();
            } catch (NullPointerException e){
                logger.fine("Could not retrieve projects");
            }
            long pluginCount = 0;
            try {
                pluginCount = Jenkins.getInstance().pluginManager.getPlugins().size();
            } catch (NullPointerException e){
                logger.fine("Could not retrieve plugins");
            }
            client.gauge("jenkins.project.count", projectCount, hostname, tags);
            client.gauge("jenkins.plugin.count", pluginCount, hostname, tags);

        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }

    }

}

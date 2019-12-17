package org.datadog.jenkins.plugins.datadog.listeners;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.SCMListener;
import hudson.scm.SCM;
import hudson.scm.SCMRevisionState;
import org.datadog.jenkins.plugins.datadog.DatadogClient;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.DatadogJobProperty;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.clients.ClientFactory;
import org.datadog.jenkins.plugins.datadog.events.SCMCheckoutCompletedEventImpl;
import org.datadog.jenkins.plugins.datadog.model.BuildData;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * This class registers an {@link SCMListener} with Jenkins which allows us to create
 * the "Checkout successful" event.
 */
@Extension
public class DatadogSCMListener extends SCMListener {

    private static final Logger logger = Logger.getLogger(DatadogSCMListener.class.getName());

    /**
     * Invoked right after the source code for the build has been checked out. It will NOT be
     * called if a checkout fails.
     *
     * @param build           - Current build
     * @param scm             - Configured SCM
     * @param workspace       - Current workspace
     * @param listener        - Current build listener
     * @param changelogFile   - Changelog
     * @param pollingBaseline - Polling
     * @throws Exception if an error is encountered
     */
    @Override
    public void onCheckout(Run<?, ?> build, SCM scm, FilePath workspace, TaskListener listener,
                           File changelogFile, SCMRevisionState pollingBaseline) throws Exception {
        try {
            // Process only if job is NOT in blacklist and is in whitelist
            DatadogJobProperty prop = DatadogUtilities.getDatadogJobProperties(build);
            if (!(DatadogUtilities.isJobTracked(build.getParent().getFullName())
                    && prop != null && prop.isEmitSCMEvents())) {
                return;
            }
            logger.fine("Start DatadogSCMListener#onCheckout");

            // Get Datadog Client Instance
            DatadogClient client = ClientFactory.getClient();

            // Collect Build Data
            BuildData buildData;
            try {
                buildData = new BuildData(build, listener);
            } catch (IOException | InterruptedException e) {
                logger.severe(e.getMessage());
                return;
            }

            // Send event
            DatadogEvent event = new SCMCheckoutCompletedEventImpl(buildData);
            client.event(event);

            // Submit counter
            String hostname = DatadogUtilities.getHostname("null");
            Map<String, Set<String>> tags = buildData.getTags();
            client.incrementCounter("jenkins.scm.checkout", hostname, tags);

            logger.fine("End DatadogSCMListener#onCheckout");
        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }
    }

}

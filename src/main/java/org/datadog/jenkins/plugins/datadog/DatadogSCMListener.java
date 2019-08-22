package org.datadog.jenkins.plugins.datadog;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.SCMListener;
import hudson.scm.SCM;
import hudson.scm.SCMRevisionState;
import org.datadog.jenkins.plugins.datadog.events.CheckoutCompletedEventImpl;
import org.datadog.jenkins.plugins.datadog.model.BuildData;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
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
        if (DatadogUtilities.isApiKeyNull()) {
            return;
        }

        // Process only if job is NOT in blacklist and is in whitelist
        DatadogJobProperty prop = DatadogUtilities.retrieveProperty(build);
        if (!(DatadogUtilities.isJobTracked(build.getParent().getFullName())
                && prop != null && prop.isEmitOnCheckout())) {
            return;
        }
        logger.fine("Checkout! in onCheckout()");

        // Instanciate the Datadog Client
        DatadogClient client = DatadogUtilities.getDatadogDescriptor().leaseDatadogClient();

        // Collect Build Data
        BuildData buildData;
        try {
            buildData = new BuildData(build, listener);
        } catch (IOException | InterruptedException e) {
            logger.severe(e.getMessage());
            return;
        }

        // Get the list of global tags to apply
        HashMap<String, String> extraTags = DatadogUtilities.buildExtraTags(build, listener);

        // Send event
        DatadogEvent event = new CheckoutCompletedEventImpl(buildData, extraTags);
        client.sendEvent(event.createPayload());
    }

}

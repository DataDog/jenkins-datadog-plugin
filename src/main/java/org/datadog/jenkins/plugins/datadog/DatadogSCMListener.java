package org.datadog.jenkins.plugins.datadog;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.SCMListener;
import hudson.scm.SCM;
import hudson.scm.SCMRevisionState;
import net.sf.json.JSONObject;

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
        String jobName = build.getParent().getFullName();
        String normalizedJobName = DatadogUtilities.normalizeFullDisplayName(jobName);
        HashMap<String, String> tags = new HashMap<>();
        DatadogJobProperty prop = DatadogUtilities.retrieveProperty(build);
        // Process only if job is NOT in blacklist and is in whitelist
        if (DatadogUtilities.isJobTracked(jobName)
                && prop != null && prop.isEmitOnCheckout()) {
            logger.fine("Checkout! in onCheckout()");

            // Get the list of global tags to apply
            tags.putAll(DatadogUtilities.getRegexJobTags(jobName));

            // Grab environment variables
            EnvVars envVars = new EnvVars();
            try {
                envVars = build.getEnvironment(listener);
                tags = DatadogUtilities.parseTagList(build, listener);
            } catch (IOException | InterruptedException e) {
                logger.severe(e.getMessage());
            }

            // Gather pre-build metadata
            JSONObject builddata = new JSONObject();
            builddata.put("hostname", DatadogUtilities.getHostname(envVars)); // string
            builddata.put("job", normalizedJobName); // string
            builddata.put("number", build.number); // int
            builddata.put("result", null); // null
            builddata.put("duration", null); // null
            builddata.put("buildurl", envVars.get("BUILD_URL")); // string
            long starttime = build.getStartTimeInMillis() / DatadogBuildListener.THOUSAND_LONG; // ms to s
            builddata.put("timestamp", starttime); // string

            DatadogEvent evt = new CheckoutCompletedEventImpl(builddata, tags);

            DatadogHttpRequests.sendEvent(evt);
        }
    }
}

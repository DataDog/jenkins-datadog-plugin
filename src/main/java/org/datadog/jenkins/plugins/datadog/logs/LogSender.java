package org.datadog.jenkins.plugins.datadog.logs;

import hudson.Extension;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogBuildListener;
import org.datadog.jenkins.plugins.datadog.DatadogClient;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.clients.DatadogHttpClient;
import org.datadog.jenkins.plugins.datadog.model.BuildData;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;


@Extension
public class LogSender extends RunListener<Run> implements Describable<DatadogBuildListener> {

    private static final Logger logger = Logger.getLogger(LogSender.class.getName());

    private String targetMetricURL = "https://api.datadoghq.com/api/";
    private Secret apiKey = null;
    private DatadogClient datadogClient;

    public LogSender() {}

    BuildData buildData;

    @Override
    public final void onCompleted(final Run run, @Nonnull final TaskListener listener) {
        if (DatadogUtilities.isApiKeyNull()) {
            return;
        }

        // Process only if job in NOT in blacklist and is in whitelist
        if (!DatadogUtilities.isJobTracked(run.getParent().getFullName())) {
            return;
        }

        try {
            buildData = new BuildData(run, listener);
        } catch (IOException | InterruptedException e) {
            logger.severe(e.getMessage());
            return;
        }

        try {
            logger.info("Attempt to send logs from build...");
            logger.info(run.getLog());
            write(Collections.singletonList(run.getLog()));
            logger.info("write method completed");
        } catch (IOException e) {
            logger.severe(e.getMessage());
            return;
        }

        logger.info("Completed build!");
    }

    @Override
    public Descriptor<DatadogBuildListener> getDescriptor() {
        return null;
    }


    private void write(List<String> lines) throws IOException {
        // For tests purposes only
        DatadogHttpClient client = new DatadogHttpClient(targetMetricURL, Secret.fromString("<the-api-key>"));
        JSONObject payload = buildPayload(buildData, lines);
        logger.info("Sending logs from write");
        logger.info(payload.toString());
        client.sendLogs(payload);
    }

    public JSONObject buildPayload(BuildData buildData, List<String> logLines) throws IOException {
        JSONObject payload = new JSONObject();
        payload.put("job", String.valueOf(buildData));
        payload.put("message", logLines);
        payload.put("ddsource", "jenkins");
        logger.info(payload.toString());
        logger.info("created the logs payload");
        return payload;
    }
}

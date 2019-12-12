package org.datadog.jenkins.plugins.datadog.events;

import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.model.BuildData;

/**
 * Class that implements the {@link DatadogEvent}. This event produces an event payload with a
 * with a proper description for a completed checkout.
 */
public class SCMCheckoutCompletedEventImpl extends AbstractDatadogBuildEvent {

    public SCMCheckoutCompletedEventImpl(BuildData buildData) {
        super(buildData);
    }

    /**
     * @return - A JSON payload. See {@link DatadogEvent#createPayload()}
     */
    @Override
    public JSONObject createPayload() {
        JSONObject payload = super.createPayload();
        String buildNumber = buildData.getBuildNumber("unknown");
        String jobName = buildData.getJobName("unknown");
        String buildUrl = buildData.getBuildUrl("unknown");
        String hostname = buildData.getHostname("unknown");

        // Build title
        // eg: `job_name build #1 checkout finished hostname`
        String title = jobName + " build #" + buildNumber + " checkout finished on " + hostname;
        payload.put("title", title);

        // Build Text
        // eg: `[Job <jobName> with build number #<buildNumber>] checkout successfully (1sec)`
        String message = "%%% \n[Job " + jobName + " build #" + buildNumber + "](" + buildUrl +
                ") checkout finished successfully on " + hostname + " " + getFormattedDuration() + " \n%%%";
        payload.put("text", message);

        payload.put("priority", "low");
        payload.put("alert_type", "success");

        return payload;
    }
}

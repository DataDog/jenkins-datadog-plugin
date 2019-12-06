package org.datadog.jenkins.plugins.datadog.events;

import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.model.BuildData;

/**
 * This event should contain all the data to construct a build started event. With
 * the right message for Datadog.
 */
public class BuildStartedEventImpl extends AbstractDatadogBuildEvent {

    public BuildStartedEventImpl(BuildData buildData) {
        super(buildData);
    }

    /**
     * @return - A JSON payload. See {@link DatadogEvent#createPayload()}
     */
    @Override
    public JSONObject createPayload() {
        JSONObject payload = super.createPayload();
        String buildNumber = buildData.getBuildNumber("unknown");
        String userId = buildData.getUserId();
        String jobName = buildData.getJobName("unknown");
        String buildUrl = buildData.getBuildUrl("unknown");
        String hostname = buildData.getHostname("unknown");

        // Build title
        // eg: `job_name build #1 started on hostname`
        String title = jobName + " build #" + buildNumber + " started on " + hostname;
        payload.put("title", title);

        // Build Text
        // eg: [User <userId> started the [job <jobName> with build number #<buildNumber>] (1sec)"
        String message = "%%% \n User " + userId + " started the [job " + jobName + " with build number #" +
                buildNumber + "](" + buildUrl + ") " + getFormattedDuration() + " \n %%%";
        payload.put("text", message);

        payload.put("priority", "low");
        payload.put("alert_type", "info");

        return payload;
    }
}

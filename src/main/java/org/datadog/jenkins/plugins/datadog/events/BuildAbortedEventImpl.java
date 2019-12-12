package org.datadog.jenkins.plugins.datadog.events;

import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.model.BuildData;

public class BuildAbortedEventImpl extends AbstractDatadogBuildEvent {

    public BuildAbortedEventImpl(BuildData buildData) {
        super(buildData);
    }

    @Override
    public JSONObject createPayload() {
        JSONObject payload = super.createPayload();
        String number = buildData.getBuildNumber("unknown");
        String userId = buildData.getUserId();
        String jobName = buildData.getJobName("unknown");
        String buildUrl = buildData.getBuildUrl("unknown");
        String hostname = buildData.getHostname("unknown");

        // Build title
        // eg: `job_name build #1 aborted on hostname`
        String title = jobName + " build #" + number + " aborted on " + hostname;
        payload.put("title", title);

        // Build Text
        // eg: `User <userId> aborted the [job with build number #<buildNumber>] (1sec)`
        String message = "%%% \nUser " + userId + " aborted the [job " + jobName + " with build number #" + number +
                "](" + buildUrl + ") " + getFormattedDuration() + " \n%%%";
        payload.put("text", message);

        payload.put("priority", "low");
        payload.put("alert_type", "info");

        return payload;
    }
}

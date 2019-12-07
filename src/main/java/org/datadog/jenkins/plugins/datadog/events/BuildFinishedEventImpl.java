package org.datadog.jenkins.plugins.datadog.events;

import hudson.model.Result;
import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.model.BuildData;

/**
 * Class that implements the {@link DatadogEvent}. This event produces an event payload
 * with a proper description for a finished build.
 */
public class BuildFinishedEventImpl extends AbstractDatadogBuildEvent {

    public BuildFinishedEventImpl(BuildData buildData) {
        super(buildData);
    }

    @Override
    public JSONObject createPayload() {
        JSONObject payload = super.createPayload();
        String buildNumber = buildData.getBuildNumber("unknown");
        String buildResult = buildData.getResult("UNKNOWN");
        String jobName = buildData.getJobName("unknown");
        String buildUrl = buildData.getBuildUrl("unknown");
        String hostname = buildData.getHostname("unknown");

        // Build title
        // eg: `job_name build #1 success on hostname`
        String title = jobName + " build #" + buildNumber + " " + buildResult.toLowerCase() + " on " + hostname;
        payload.put("title", title);

        // Build Text
        // eg: `[Job <jobName> with build number #<buildNumber>] finished with status <buildResult> (1sec)`
        String message = "%%% \n[Job " + jobName + " build #" + buildNumber + "](" + buildUrl +
                ") finished with status " + buildResult.toLowerCase() + " " + getFormattedDuration() + " \n%%%";
        payload.put("text", message);

        if (Result.SUCCESS.toString().equals(buildResult)) {
            payload.put("priority", "low");
            payload.put("alert_type", "success");
        } else if (Result.FAILURE.toString().equals(buildResult)) {
            payload.put("priority", "normal");
            payload.put("alert_type", "error");
        } else {
            payload.put("priority", "normal");
            payload.put("alert_type", "warning");
        }

        return payload;
    }
}

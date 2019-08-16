package org.datadog.jenkins.plugins.datadog;

import hudson.model.Result;
import net.sf.json.JSONObject;

import java.util.Map;

/**
 * Class that implements the {@link DatadogEvent}. This event produces an event payload with a
 * with a proper description for a finished build.
 */
public class BuildFinishedEventImpl extends AbstractDatadogEvent {

    public BuildFinishedEventImpl(JSONObject buildData, Map<String, String> buildTags) {
        super(buildData, buildTags);
    }

    @Override
    public JSONObject createPayload() {
        JSONObject payload = createPayload("build result");
        String hostname = DatadogUtilities.nullSafeGetString(builddata, "hostname");
        String number = DatadogUtilities.nullSafeGetString(builddata, "number");
        String buildurl = DatadogUtilities.nullSafeGetString(builddata, "buildurl");
        String job = DatadogUtilities.nullSafeGetString(builddata, "job");
        String buildResult = builddata.get("result") != null ? builddata.get("result").toString() : Result.NOT_BUILT.toString();

        // Build title
        StringBuilder title = new StringBuilder();
        title.append(job).
                append(" build #").
                append(number).
                append(" " + buildResult.toLowerCase()).
                append(" on ").
                append(hostname);
        payload.put("title", title.toString());

        StringBuilder message = new StringBuilder();
        message.append("%%% \n [See results for build #").
                append(number).
                append("](").
                append(buildurl).
                append(") ").
                append(getDuration()).
                append(" \n %%%");
        payload.put("text", message.toString());

        if (Result.SUCCESS.toString().equals(buildResult)) {
            payload.put("priority", "low");
            payload.put("alert_type", "success");
        } else if (Result.UNSTABLE.toString().equals(buildResult) ||
                Result.ABORTED.toString().equals(buildResult) ||
                Result.NOT_BUILT.toString().equals(buildResult)) {
            payload.put("alert_type", "warning");
        } else if (Result.FAILURE.toString().equals(buildResult)) {
            payload.put("alert_type", "error");
        }

        return payload;
    }
}

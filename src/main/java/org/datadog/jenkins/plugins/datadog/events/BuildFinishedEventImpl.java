package org.datadog.jenkins.plugins.datadog.events;

import hudson.model.Result;
import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.model.BuildData;

import java.util.Map;

/**
 * Class that implements the {@link DatadogEvent}. This event produces an event payload with a
 * with a proper description for a finished build.
 */
public class BuildFinishedEventImpl extends AbstractDatadogEvent {

    public BuildFinishedEventImpl(BuildData buildData, Map<String, String> buildTags) {
        super(buildData, buildTags);
    }

    @Override
    public JSONObject createPayload() {
        JSONObject payload = super.createPayload();
        String number = builddata.getNumber(null) == null ?
                "unknown" : builddata.getNumber(null).toString();
        String buildResult = builddata.getResult("UNKNOWN");

        // Build title
        StringBuilder title = new StringBuilder();
        title.append(builddata.getJob("unknown")).
                append(" build #").
                append(number).
                append(" ").
                append(buildResult.toLowerCase()).
                append(" on ").
                append(builddata.getHostname("unknown"));
        payload.put("title", title.toString());

        StringBuilder message = new StringBuilder();
        message.append("%%% \n [See results for build #").
                append(number).
                append("](").
                append(builddata.getBuildUrl("unknown")).
                append(") ").
                append(getDuration()).
                append(" \n %%%");
        payload.put("text", message.toString());

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

package org.datadog.jenkins.plugins.datadog.events;

import hudson.model.Result;
import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.model.BuildData;

import java.util.Map;

/**
 * Class that implements the {@link DatadogEvent}. This event produces an event payload
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
        String title = builddata.getJob("unknown") +
                " build #" +
                number +
                " " +
                buildResult.toLowerCase() +
                " on " +
                builddata.getHostname("unknown");
        payload.put("title", title);

        String message = "%%% \n [See results for build #" +
                number +
                "](" +
                builddata.getBuildUrl("unknown") +
                ") " +
                getDuration() +
                " \n %%%";
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

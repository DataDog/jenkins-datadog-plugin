package org.datadog.jenkins.plugins.datadog;

import net.sf.json.JSONObject;

import java.util.Map;

/**
 * This event should contain all the data to construct a build started event. With
 * the right message for Datadog.
 */
public class BuildStartedEventImpl extends AbstractDatadogEvent {

    public BuildStartedEventImpl(JSONObject buildData, Map<String, String> buildTags) {
        super(buildData, buildTags);
    }

    /**
     * @return - A JSON payload. See {@link DatadogEvent#createPayload()}
     */
    @Override
    public JSONObject createPayload() {
        JSONObject payload = createPayload("build start");
        String hostname = DatadogUtilities.nullSafeGetString(builddata, "hostname");
        String number = DatadogUtilities.nullSafeGetString(builddata, "number");
        String buildurl = DatadogUtilities.nullSafeGetString(builddata, "buildurl");
        String job = DatadogUtilities.nullSafeGetString(builddata, "job");

        // Build title
        StringBuilder title = new StringBuilder();
        title.append(job).append(" build #").append(number).append(" started").append(" on ").append(hostname);
        payload.put("title", title.toString());

        // Build Text
        StringBuilder message = new StringBuilder();
        message.append("%%% \n [Follow build #").
                append(number).
                append(" progress](").
                append(buildurl).
                append(") ").
                append(getDuration()).
                append(" \n %%%");
        payload.put("text", message.toString());

        payload.put("alert_type", "info");
        payload.put("priority", "low");
        payload.put("event_type", builddata.get("event_type"));

        return payload;
    }
}

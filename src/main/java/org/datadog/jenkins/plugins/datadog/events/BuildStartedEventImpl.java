package org.datadog.jenkins.plugins.datadog.events;

import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.model.BuildData;

import java.util.Map;

/**
 * This event should contain all the data to construct a build started event. With
 * the right message for Datadog.
 */
public class BuildStartedEventImpl extends AbstractDatadogEvent {

    public BuildStartedEventImpl(BuildData buildData, Map<String, String> buildTags) {
        super(buildData, buildTags);
    }

    /**
     * @return - A JSON payload. See {@link DatadogEvent#createPayload()}
     */
    @Override
    public JSONObject createPayload() {
        JSONObject payload = super.createPayload();
        String number = builddata.getNumber(null) == null ?
                "unknown" : builddata.getNumber(null).toString();

        // Build title
        StringBuilder title = new StringBuilder();
        title.append( builddata.getJob("unknown")).
                append(" build #").
                append(number).
                append(" started").
                append(" on ").
                append(builddata.getHostname("unknown"));
        payload.put("title", title.toString());

        // Build Text
        StringBuilder message = new StringBuilder();
        message.append("%%% \n [Follow build #").
                append(number).
                append(" progress](").
                append(builddata.getBuildUrl("unknown")).
                append(") ").
                append(getDuration()).
                append(" \n %%%");
        payload.put("text", message.toString());

        payload.put("alert_type", "info");
        payload.put("priority", "low");

        return payload;
    }
}

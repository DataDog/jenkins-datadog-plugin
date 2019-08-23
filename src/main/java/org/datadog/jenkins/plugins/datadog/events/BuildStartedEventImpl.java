package org.datadog.jenkins.plugins.datadog.events;

import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
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
        String title = builddata.getJob("unknown") +
                " build #" +
                number +
                " started" +
                " on " +
                builddata.getHostname("unknown");
        payload.put("title", title);

        // Build Text
        String message = "%%% \n [Follow build #" +
                number +
                " progress](" +
                builddata.getBuildUrl("unknown") +
                ") " +
                getDuration() +
                " \n %%%";
        payload.put("text", message);

        payload.put("alert_type", "info");
        payload.put("priority", "low");

        return payload;
    }
}

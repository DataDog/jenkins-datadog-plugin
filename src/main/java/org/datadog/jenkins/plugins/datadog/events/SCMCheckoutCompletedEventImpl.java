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
        String number = buildData.getBuildNumber("unknown");

        // Build title
        String title = buildData.getJobName("unknown") +
                " build #" +
                number +
                " checkout finished" +
                " on " +
                buildData.getHostname("unknown");
        payload.put("title", title);

        // Build Text
        String message = "%%% \n [Follow build #" +
                number +
                " progress](" +
                buildData.getBuildUrl("unknown") +
                ") " +
                getFormattedDuration() +
                " \n %%%";
        payload.put("text", message);

        payload.put("priority", "low");
        payload.put("alert_type", "info");

        return payload;
    }
}

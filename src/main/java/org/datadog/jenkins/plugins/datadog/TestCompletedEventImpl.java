package org.datadog.jenkins.plugins.datadog;

import java.util.Map;
import net.sf.json.JSONObject;

/**
 *
 * This event should contain all the data to construct a test completed event.
 * With the right message for Datadog.
 */
public class TestCompletedEventImpl implements DatadogEvent {

    static final String TEST_DURATION = "testduration";
    static final String TEST_URL = "testurl";
    static final String TEST_NAME = "testname";

    private JSONObject builddata;
    private Map<String, String> tags;

    public TestCompletedEventImpl(JSONObject buildData, Map<String, String> tags) {
        this.builddata = buildData;
        this.tags = tags;
    }

    /**
     *
     * @return - A JSON payload. See {@link DatadogEvent#createPayload()}
     */
    @Override
    public JSONObject createPayload() {
        JSONObject payload = new JSONObject();
        // Add event_type to assist in roll-ups
        payload.put("event_type", "test completed"); // string
        String hostname = DatadogUtilities.nullSafeGetString(builddata, "hostname");
        String testurl = DatadogUtilities.nullSafeGetString(builddata, TEST_URL);
        String job = DatadogUtilities.nullSafeGetString(builddata, TEST_NAME);
        long timestamp = builddata.getLong("timestamp");
        String message = "";

        // Build title
        StringBuilder title = new StringBuilder();
        title.append(job).append(" test completed");
        title.append(" started");
        message = "%%% \n [Follow test results(" + testurl + ") ";

        title.append(" on ").append(hostname);
        // Add duration
        if (builddata.get(TEST_DURATION) != null) {
            message = message + DatadogUtilities.durationToString(builddata.getDouble(TEST_DURATION));
        }

        // Close markdown
        message = message + " \n %%%";

        // Build payload
        payload.put("alert_type", "info");
        payload.put("priority", "low");
        payload.put("title", title.toString());
        payload.put("text", message);
        payload.put("date_happened", timestamp);
        payload.put("event_type", builddata.get("event_type"));
        payload.put("host", hostname);
        payload.put("result", builddata.get("result"));
        payload.put("tags", DatadogUtilities.assembleTags(builddata, tags));
        payload.put("aggregation_key", job);
        payload.put("source_type_name", "jenkins");

        return payload;
    }
}

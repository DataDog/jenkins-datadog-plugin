package org.datadog.jenkins.plugins.datadog;

import net.sf.json.JSONObject;

import java.util.Map;

public abstract class AbstractDatadogEvent implements DatadogEvent {

    protected JSONObject builddata;
    protected Map<String, String> tags;

    public AbstractDatadogEvent(JSONObject buildData, Map<String, String> buildTags) {
        this.builddata = buildData;
        this.tags = buildTags;
    }

    public JSONObject createPayload(String eventType) {
        JSONObject payload = new JSONObject();
        // Add event_type to assist in roll-ups
        payload.put("event_type", eventType);
        String hostname = DatadogUtilities.nullSafeGetString(builddata, "hostname");
        payload.put("host", hostname);
        String job = DatadogUtilities.nullSafeGetString(builddata, "job");
        payload.put("aggregation_key", job);
        payload.put("date_happened", builddata.getLong("timestamp"));
        payload.put("result", builddata.get("result"));
        payload.put("tags", DatadogUtilities.assembleTags(builddata, tags));
        payload.put("source_type_name", "jenkins");

        return payload;
    }

    protected String getDuration() {
        if (builddata.get("duration") != null) {
            return DatadogUtilities.durationToString(builddata.getDouble("duration"));
        }else{
            return "";
        }
    }
}

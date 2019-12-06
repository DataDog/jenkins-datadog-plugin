package org.datadog.jenkins.plugins.datadog.events;

import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.model.BuildData;
import org.datadog.jenkins.plugins.datadog.util.TagsUtil;

public abstract class AbstractDatadogBuildEvent implements DatadogEvent {

    protected BuildData buildData;

    private static final float MINUTE = 60;
    private static final float HOUR = 3600;

    public AbstractDatadogBuildEvent(BuildData buildData) {
        this.buildData = buildData;
    }

    public JSONObject createPayload() {
        JSONObject payload = new JSONObject();
        payload.put("host", buildData.getHostname(null));
        payload.put("aggregation_key", buildData.getJobName(null));
        payload.put("date_happened", buildData.getEndTime(System.currentTimeMillis()) / 1000);
        payload.put("tags", TagsUtil.convertTagsToJSONArray(buildData.getTags()));
        payload.put("source_type_name", "jenkins");

        return payload;
    }

    protected String getFormattedDuration() {
        Long duration = buildData.getDuration(null);
        if (duration != null) {
            String output = "(";
            String format = "%.2f";
            double d = duration.doubleValue() / 1000;
            if (d < MINUTE) {
                output = output + String.format(format, d) + " secs)";
            } else if (MINUTE <= d && d < HOUR) {
                output = output + String.format(format, d / MINUTE) + " mins)";
            } else if (HOUR <= d) {
                output = output + String.format(format, d / HOUR) + " hrs)";
            }
            return output;
        } else {
            return "";
        }
    }
}

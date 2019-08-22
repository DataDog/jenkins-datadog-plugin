package org.datadog.jenkins.plugins.datadog.events;

import hudson.model.Result;
import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogBuildListener;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.model.BuildData;

import java.util.Map;

public abstract class AbstractDatadogEvent implements DatadogEvent {

    protected BuildData builddata;
    protected Map<String, String> tags;

    private static final float MINUTE = 60;
    private static final float HOUR = 3600;

    public AbstractDatadogEvent(BuildData buildData, Map<String, String> buildTags) {
        this.builddata = buildData;
        this.tags = buildTags;
    }

    public JSONObject createPayload() {
        JSONObject payload = new JSONObject();
        payload.put("host", builddata.getHostname(null));
        payload.put("aggregation_key", builddata.getJob(null));
        payload.put("date_happened", builddata.getTimestamp(System.currentTimeMillis() / 1000));
        payload.put("tags", builddata.getAssembledTags(tags));
        payload.put("source_type_name", "jenkins");

        return payload;
    }

    protected String getDuration() {
        Long duration = builddata.getDuration(null);
        if (duration != null) {
            String output = "(";
            String format = "%.2f";
            double d = duration.doubleValue();
            if (d < MINUTE) {
                output = output + String.format(format, d) + " secs)";
            } else if (MINUTE <= d && d < HOUR) {
                output = output + String.format(format, d / MINUTE) + " mins)";
            } else if (HOUR <= d) {
                output = output + String.format(format, d / HOUR) + " hrs)";
            }
            return output;
        }else{
            return "";
        }
    }
}
